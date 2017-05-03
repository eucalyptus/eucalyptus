/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.network.config

import com.eucalyptus.address.Addresses
import com.eucalyptus.bootstrap.Bootstrap
import com.eucalyptus.bootstrap.Databases
import com.eucalyptus.bootstrap.Hosts
import com.eucalyptus.component.Faults
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.configurable.ConfigurableProperty
import com.eucalyptus.configurable.ConfigurablePropertyException
import com.eucalyptus.configurable.PropertyChangeListener
import com.eucalyptus.event.ClockTick
import com.eucalyptus.event.Listeners
import com.eucalyptus.event.EventListener as EucaEventListener
import com.eucalyptus.network.DispatchingNetworkingService
import com.eucalyptus.network.IPRange
import com.eucalyptus.network.NetworkGroups
import com.eucalyptus.network.NetworkMode
import com.eucalyptus.network.PrivateAddresses
import com.eucalyptus.util.Exceptions
import com.eucalyptus.util.Json
import com.eucalyptus.util.Pair
import com.eucalyptus.vm.VmInstances
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.google.common.base.Optional
import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.apache.log4j.Logger
import org.springframework.context.MessageSource
import org.springframework.context.support.StaticMessageSource
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.ValidationUtils

import javax.annotation.Nullable
import java.util.concurrent.atomic.AtomicReference

/**
 *
 */
@SuppressWarnings("UnnecessaryQualifiedReference")
@CompileStatic
class NetworkConfigurations {
  private static final Logger logger = Logger.getLogger( NetworkConfigurations )
  private static final boolean validateConfiguration =
      Boolean.valueOf( System.getProperty( 'com.eucalyptus.network.config.validateNetworkConfiguration', 'true' ) )
  private static final AtomicReference<Supplier<Void>> managedFaultSupplier =
      new AtomicReference<>( Suppliers.memoize( managedFaultSupplier( ) ) );

  static Optional<NetworkConfiguration> getNetworkConfiguration( ) {
    NetworkConfigurations.networkConfigurationFromProperty
  }

  static Optional<NetworkConfiguration> getNetworkConfigurationFromProperty( ) {
    Optional<NetworkConfiguration> configuration = Optional.absent( )
    String configurationText = NetworkGroups.NETWORK_CONFIGURATION
    if ( !Strings.isNullOrEmpty( configurationText ) ) {
      try {
        Optional<NetworkConfiguration> parsedConfiguration = Optional.of( parse( configurationText ) );
        if ( parsedConfiguration.get( ).managedSubnet ) {
          managedFaultSupplier.get( ).get( );
        } else {
          managedFaultSupplier.set( Suppliers.memoize( managedFaultSupplier( ) ) )
          configuration = parsedConfiguration
        }
      } catch ( NetworkConfigurationException e ) {
        throw Exceptions.toUndeclared( e )
      }
    }
    configuration
  }

  static List<String> loadSystemNameservers( final List<String> fallback ) {
    final String dnsServersProperty = SystemConfiguration.getSystemConfiguration( ).getNameserverAddress( )
    '127.0.0.1'.equals( dnsServersProperty ) ?
        fallback :
        Lists.newArrayList( Splitter.on(',').omitEmptyStrings().trimResults().split( dnsServersProperty ) ) as List<String> ?: fallback
  }

  @PackageScope
  static void process( final NetworkConfiguration networkConfiguration ) {
    Addresses.getInstance( ).update( iterateRangesAsString( networkConfiguration.publicIps ) )
  }

  @Nullable
  static EdgeSubnet getSubnetForCluster( NetworkConfiguration configuration, String clusterName ) {
    EdgeSubnet defaultSubnet = 1==(configuration?.subnets?.size()?:0) ? configuration?.subnets[0] : null
    Cluster cluster = configuration.clusters?.find{ Cluster cluster -> clusterName == cluster.name }
    EdgeSubnet clusterSubnetFallback = cluster?.subnet?.name ?
        configuration?.subnets?.find{ EdgeSubnet subnet -> (subnet.name?:subnet.subnet)==cluster?.subnet?.name }?:defaultSubnet :
        defaultSubnet
    String name = cluster?.subnet?.name?:cluster?.subnet?.subnet?:clusterSubnetFallback?.name?:clusterSubnetFallback?.subnet
    String subnet = cluster?.subnet?.subnet?:clusterSubnetFallback?.subnet
    String netmask = cluster?.subnet?.netmask?:clusterSubnetFallback?.netmask
    String gateway = cluster?.subnet?.gateway?:clusterSubnetFallback?.gateway
    subnet && netmask && gateway ? new EdgeSubnet(
      name: name,
      subnet: subnet,
      netmask: netmask,
      gateway: gateway
    ) : null
  }

  static Collection<String> getPrivateAddressRanges( NetworkConfiguration configuration, String clusterName ) {
    NetworkConfigurations.explode( configuration, [ clusterName ] ).with{ NetworkConfiguration exploded ->
      exploded.clusters.find{ Cluster cluster -> cluster.name == clusterName }.with{ Cluster cluster ->
        if ( cluster.subnet == null ) {
          // We check the subnet since if this is not configured the
          // request will fail on the back end
          throw new IllegalStateException( "Networking configuration not found for cluster '${clusterName}'" )
        }

        cluster.privateIps ? cluster.privateIps : [ ] as List<String>
      }
    }
  }

  static String getMacPrefix( ) {
    getMacPrefix( NetworkConfigurations.networkConfiguration )
  }

  static String getMacPrefix( final Optional<NetworkConfiguration> configuration ) {
    String macPrefix = VmInstances.MAC_PREFIX
    if ( configuration.isPresent( ) ) {
      final NetworkConfiguration exploded = explode( configuration.get( ) )
      if ( 1 == exploded?.clusters?.size( ) && exploded.clusters[0].macPrefix ) {
        macPrefix = exploded.clusters[0].macPrefix
      } else if ( exploded.macPrefix ) {
        macPrefix = exploded.macPrefix
      }
    }
    macPrefix
  }

  static Pair<Iterable<Integer>,Integer> getPrivateAddresses( NetworkConfiguration configuration, String clusterName ) {
    NetworkConfigurations.iterateRanges( NetworkConfigurations.getPrivateAddressRanges( configuration, clusterName ) )
  }

  static Pair<Iterable<Integer>,Integer> getPrivateAddresses( String clusterName ) {
    Optional<NetworkConfiguration> configuration = NetworkConfigurations.networkConfiguration
    if ( !configuration.present ) {
      throw new IllegalStateException( "Networking configuration not found for cluster '${clusterName}'" )
    }
    NetworkConfigurations.getPrivateAddresses( configuration.get( ), clusterName )
  }

  static NetworkConfiguration parse( final String configuration ) throws NetworkConfigurationException {
    final ObjectMapper mapper = Json.mapper( )
    mapper.setPropertyNamingStrategy( PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE )
    final NetworkConfiguration networkConfiguration
    try {
      networkConfiguration = mapper.readValue( new StringReader( configuration ){
        @Override String toString() { "property" } // overridden for better source in error message
      }, NetworkConfiguration.class )
    } catch ( JsonProcessingException e ) {
      throw new NetworkConfigurationException( e.getMessage( ) )
    }
    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult( networkConfiguration, "NetworkConfiguration");
    ValidationUtils.invokeValidator( new NetworkConfigurationValidator(errors), networkConfiguration, errors )
    if ( validateConfiguration && errors.hasErrors( ) ) {
      MessageSource source = new StaticMessageSource( ) // default messages will be used
      throw new NetworkConfigurationException( source.getMessage( errors.getAllErrors( ).get( 0 ), Locale.getDefault( ) ) )
    }
    networkConfiguration
  }

  /**
   * Create a deep copy of the given network configuration.
   *
   * @param configuration The configuration to copy.
   * @return The copy.
   */
  static NetworkConfiguration copyOf( final NetworkConfiguration configuration ) {
    final ObjectMapper mapper = new ObjectMapper( )
    mapper.treeToValue( mapper.valueToTree( configuration ), NetworkConfiguration )
  }

  /**
   * Explode the configuration by populating all values at the cluster level
   * including filling in default values where possible.
   *
   * @param networkConfiguration The configuration to explode.
   * @param clusterNames The required clusters in the result
   * @return The exploded configuration.
   */
  static NetworkConfiguration explode( final NetworkConfiguration networkConfiguration,
                                       final Iterable<String> clusterNames = [ ] ) {
    NetworkConfiguration configuration = copyOf( networkConfiguration )

    List<Cluster> clusters = configuration.clusters ?: [ ] as List<Cluster>
    configuration.clusters = clusters

    // Ensure required clusters exist
    clusters.collect{ Cluster cluster -> cluster.name }.with{ List<String> names ->
      clusters.addAll( ((Iterable) clusterNames).findResults{ Object requiredName ->
        names.contains( requiredName ) ?
            null:
            new Cluster( name: requiredName as String )
      } )
    }

    // Populate values at cluster level
    clusters.each{ Cluster cluster ->  cluster.with{
      if ( !macPrefix ) {
        macPrefix = configuration.macPrefix?:VmInstances.MAC_PREFIX
      }

      // EDGE mode configuration
      if ( configuration.mode == null || 'EDGE' == configuration.mode ) {
        if ( ( subnet == null || subnet && subnet.name && !subnet.subnet ) && name ) {
          subnet = NetworkConfigurations.getSubnetForCluster( configuration, name )
        }
      }

      // Groovy gets confused here so we use the "cluster." prefix for the privateIps property
      if ( cluster.privateIps == null && configuration.privateIps ) {
        cluster.privateIps = configuration.privateIps
      } else if ( !cluster.privateIps && subnet ) {
        if ( subnet.gateway && !subnet.gateway.isEmpty( ) && !subnet.gateway.isAllWhitespace( ) ) {
          cluster.privateIps = subnet.with {IPRange.fromSubnet(subnet, netmask).split(gateway).collect { IPRange range -> range.toString() }}
        } else {
          cluster.privateIps = [ ] as List<String>
        }
      }

      void
    } }

    // Remove any global subnets that are used by clusters
    Collection<String> clusterSubnetsAndNetmasks = clusters.collect{ Cluster cluster ->
      "${cluster?.subnet?.subnet}/${cluster?.subnet?.netmask}".toString( )
    }
    configuration.subnets?.removeAll{ EdgeSubnet subnet ->
      clusterSubnetsAndNetmasks.contains( "${subnet?.subnet}/${subnet?.netmask}".toString( ) )
    }
    if ( configuration.subnets?.isEmpty( ) ) {
      configuration.subnets = null
    }

    configuration
  }

  /**
   * Validate the given configuration
   *
   * <p>Validate the configuration as populated. This will not validate absent
   * properties, if this is desired the configuration should first be
   * exploded.</p>
   *
   * <p>This method currently validates for EDGE mode:</p>
   *
   * <ul>
   *   <li>Exactly one subnet (with valid IPs) is configured if there are no clusters specified</li>
   *   <li>Each cluster has valid private IPs for its subnet.</li>
   * </ul>
   *
   * <p>Lower level validation is performed when parsing, this method will only
   * perform the semantic validation described above.</p>
   *
   * @see #explode( NetworkConfiguration )
   */
  static void validate( final NetworkConfiguration configuration ) throws NetworkConfigurationException {
    configuration?.with{
      if ( configuration.mode == null || 'EDGE' == configuration.mode ) {
        if ( configuration.clusters ) {
          configuration.clusters.each{ Cluster cluster ->
            if ( cluster.subnet && cluster.privateIps ) {
              validateIPsForSubnet( cluster.subnet, cluster.privateIps, " for cluster ${cluster.name}" )
            }
            void
          }
        } else {
          if ( !configuration.subnets || configuration.subnets.size( ) != 1 ) {
            throw new NetworkConfigurationException('A single subnet must be configured when clusters are not specified.' )
          }
          validateIPsForSubnet( configuration.subnets[0], configuration.privateIps, '' )
        }
      } else if ( 'VPCMIDO' == configuration.mode ) {
        if ( configuration.clusters ) {
          configuration.clusters.each { Cluster cluster ->
            if ( cluster.privateIps ) {
              throw new NetworkConfigurationException('Private IP configuration not permitted for VPCMIDO.' )
            }
            if ( cluster.subnet ) {
              throw new NetworkConfigurationException('Subnet configuration not permitted for VPCMIDO.' )
            }
            void
          }
        }
        if ( configuration.privateIps ) {
          throw new NetworkConfigurationException('Private IP configuration not permitted for VPCMIDO.' )
        }
        if ( configuration.subnets ) {
          throw new NetworkConfigurationException('Subnet configuration not permitted for VPCMIDO.' )
        }
      }
      void
    }
  }

  private static void validateIPsForSubnet( final Subnet net,
                                            final List<String> ipRanges,
                                            final String desc = '' ) {
    String subnet = net.subnet
    String netmask = net.netmask
    IPRange subnetRange = IPRange.fromSubnet( subnet, netmask )
    ipRanges?.collect{ String range -> IPRange.parse( ).apply( range ) }?.findResults{ Optional<IPRange> range -> range.orNull( ) }?.each{ IPRange range ->
      if ( !subnetRange.contains( range ) ) {
        throw new NetworkConfigurationException( "Private IP range ${range} not valid for subnet/netmask ${subnet}/${netmask}${desc}" )
      }
    }

  }

  private static Pair<Iterable<Integer>,Integer> iterateRanges( Iterable<String> rangeIterable ) {
    final List<IPRange> ranges = Lists.newArrayList( Optional.presentInstances( Iterables.transform( rangeIterable, IPRange.parse( ) ) ) );
    int rangesSize = 0
    for ( IPRange range : ranges ) { rangesSize = rangesSize + (int) range.size( ) }
    Pair.pair( Iterables.concat( ranges ), rangesSize )
  }

  private static Iterable<String> iterateRangesAsString( Iterable<String> rangeIterable ) {
    Iterables.transform( iterateRanges( rangeIterable ).left, PrivateAddresses.fromInteger( ) )
  }

  private static Supplier<Void> managedFaultSupplier( ) {
    return {
      logger.error( "Managed network modes not supported, network configuration ignored" )
      Faults.forComponent( Eucalyptus ).havingId( 1017 ).log( )
      void
    } as Supplier<Void>
  }

  static class NetworkConfigurationPropertyChangeListener implements PropertyChangeListener<String> {
    @Override
    void fireChange( final ConfigurableProperty property,
                     final String newValue ) throws ConfigurablePropertyException {
      if ( !Strings.isNullOrEmpty( newValue ) ) {
        try {
          validate( explode( parse( newValue ) ) )
        } catch ( NetworkConfigurationException e ) {
          throw e
        } catch ( e ) {
          throw new ConfigurablePropertyException( e.getMessage( ), e )
        }
      }
    }
  }

  public static class NetworkConfigurationEventListener implements EucaEventListener<ClockTick> {
    public static void register( ) {
      Listeners.register( ClockTick.class, new NetworkConfigurationEventListener( ) )
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      if ( !Bootstrap.isShuttingDown( ) && !Databases.isVolatile( ) ) {
        try {
          Optional<NetworkConfiguration> configurationOptional = NetworkConfigurations.networkConfiguration
          if ( configurationOptional.present ) {
            DispatchingNetworkingService.updateNetworkService(
                NetworkMode.fromString( configurationOptional.get( ).mode, NetworkMode.EDGE ) )
            if ( Hosts.isCoordinator( ) ) {
              configurationOptional.orNull( )?.with{ NetworkConfiguration networkConfiguration ->
                NetworkConfigurations.process( networkConfiguration )
              }
            }
          }
        } catch ( e ) {
          logger.error( "Error updating network configuration", e )
        }
      }
    }
  }
}
