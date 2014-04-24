/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
import com.eucalyptus.cluster.ClusterConfiguration
import com.eucalyptus.component.Components
import com.eucalyptus.component.id.ClusterController
import com.eucalyptus.configurable.ConfigurableProperty
import com.eucalyptus.configurable.ConfigurablePropertyException
import com.eucalyptus.configurable.PropertyChangeListener
import com.eucalyptus.entities.Entities
import com.eucalyptus.event.ClockTick
import com.eucalyptus.event.Listeners
import com.eucalyptus.event.EventListener as EucaEventListener
import com.eucalyptus.network.EdgeNetworking
import com.eucalyptus.network.IPRange
import com.eucalyptus.network.NetworkGroups
import com.eucalyptus.network.PrivateAddresses
import com.eucalyptus.util.Exceptions
import com.google.common.base.Optional
import com.google.common.base.Predicate
import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.apache.log4j.Logger
import org.codehaus.jackson.JsonProcessingException
import org.codehaus.jackson.map.ObjectMapper
import org.springframework.context.MessageSource
import org.springframework.context.support.StaticMessageSource
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.ValidationUtils

import javax.annotation.Nullable
import javax.persistence.EntityTransaction
import java.util.concurrent.TimeUnit

/**
 *
 */
@SuppressWarnings("UnnecessaryQualifiedReference")
@CompileStatic
class NetworkConfigurations {
  private static final Logger logger = Logger.getLogger( NetworkConfigurations )
  private static final Supplier<Optional<NetworkConfiguration>> networkConfigurationFromEnvironmentSupplier =
      Suppliers.memoizeWithExpiration(
          NetworkConfigurations.&loadNetworkConfigurationFromEnvironment as Supplier<Optional<NetworkConfiguration>>,
          5,
          TimeUnit.MINUTES
      )
  private static final boolean validateConfiguration =
      Boolean.valueOf( System.getProperty( 'com.eucalyptus.network.config.validateNetworkConfiguration', 'true' ) )

  static Optional<NetworkConfiguration> getNetworkConfiguration( ) {
    NetworkConfigurations.networkConfigurationFromProperty.or(
        NetworkConfigurations.networkConfigurationFromEnvironmentSupplier.get( ) )
  }

  static Optional<NetworkConfiguration> getNetworkConfigurationFromProperty( ) {
    Optional<NetworkConfiguration> configuration = Optional.absent( )
    String configurationText = NetworkGroups.NETWORK_CONFIGURATION
    if ( !Strings.isNullOrEmpty( configurationText ) ) {
      try {
        configuration = Optional.of( parse( configurationText ) )
      } catch ( NetworkConfigurationException e ) {
        throw Exceptions.toUndeclared( e )
      }
    }
    configuration
  }

  static Optional<NetworkConfiguration> getNetworkConfigurationFromEnvironment( ) {
    networkConfigurationFromEnvironmentSupplier.get( )
  }

  @PackageScope
  static List<String> loadSystemNameservers( final List<String> fallback ) {
    final String dnsServersProperty = SystemConfiguration.getSystemConfiguration( ).getNameserverAddress( )
    '127.0.0.1'.equals( dnsServersProperty ) ?
        fallback :
        Lists.newArrayList( Splitter.on(',').omitEmptyStrings().trimResults().split( dnsServersProperty ) ) as List<String> ?: fallback
  }

  @PackageScope
  static Optional<NetworkConfiguration> loadNetworkConfigurationFromEnvironment( ) {
    buildNetworkConfigurationFromProperties(
        loadSystemNameservers( [] as List<String> ),
        loadEucalyptusConf( ).collectEntries( Maps.<String,String>newHashMap( ) ){
      Object key, Object value -> [ String.valueOf(key), String.valueOf(value) ]
    } )
  }

  @PackageScope
  static void process( final NetworkConfiguration networkConfiguration ) {
    Addresses.addressManager.update( iterateRangesAsString( networkConfiguration.publicIps ) )
    Entities.transaction( ClusterConfiguration.class ) { EntityTransaction db ->
      Components.lookup(ClusterController.class).services().each { ClusterConfiguration config ->
        (networkConfiguration?.clusters?.find{ Cluster cluster -> cluster.name == config.partition }?:new Cluster()).with{
          ClusterConfiguration clusterConfiguration = Entities.uniqueResult( config )
          clusterConfiguration.networkMode = 'EDGE'
          clusterConfiguration.addressesPerNetwork = -1
          clusterConfiguration.useNetworkTags = false
          clusterConfiguration.minNetworkTag = -1
          clusterConfiguration.maxNetworkTag = -1
          clusterConfiguration.minNetworkIndex = -1
          clusterConfiguration.maxNetworkIndex = -1

          Subnet defaultSubnet = null
          if ( subnet && subnet.name ) {
            defaultSubnet = networkConfiguration.subnets?.find{ Subnet s -> s.name?:s.subnet == subnet.name }
          } else if ( !subnet ) {
            defaultSubnet = networkConfiguration.subnets?.getAt(0) // must be only one
          }

          clusterConfiguration.vnetSubnet = subnet?.subnet?:defaultSubnet?.subnet
          clusterConfiguration.vnetNetmask = subnet?.netmask?:defaultSubnet?.netmask
        }
      }
      db.commit( );
    }
  }

  @Nullable
  static Subnet getSubnetForCluster( NetworkConfiguration configuration, String clusterName ) {
    Subnet defaultSubnet = 1==(configuration?.subnets?.size()?:0) ? configuration?.subnets[0] : null
    Cluster cluster = configuration.clusters?.find{ Cluster cluster -> clusterName == cluster.name }
    Subnet clusterSubnetFallback = cluster?.subnet?.name ?
        configuration?.subnets?.find{ Subnet subnet -> (subnet.name?:subnet.subnet)==cluster?.subnet?.name }?:defaultSubnet :
        defaultSubnet
    String name = cluster?.subnet?.name?:cluster?.subnet?.subnet?:clusterSubnetFallback?.name?:clusterSubnetFallback?.subnet
    String subnet = cluster?.subnet?.subnet?:clusterSubnetFallback?.subnet
    String netmask = cluster?.subnet?.netmask?:clusterSubnetFallback?.netmask
    String gateway = cluster?.subnet?.gateway?:clusterSubnetFallback?.gateway
    subnet && netmask && gateway ? new Subnet(
      name: name,
      subnet: subnet,
      netmask: netmask,
      gateway: gateway
    ) : null
  }

  static Collection<String> getPrivateAddressRanges( NetworkConfiguration configuration, String clusterName ) {
    NetworkConfigurations.explode( configuration, [ clusterName ] ).with{ NetworkConfiguration exploded ->
      exploded.clusters.find{ Cluster cluster -> cluster.name == clusterName }.with{ Cluster cluster ->
        if ( cluster.subnet == null || !cluster.privateIps ) {
          // We check the subnet since if this is not configured the
          // request will fail on the back end
          throw new IllegalStateException( "Networking configuration not found for cluster '${clusterName}'" )
        }
        cluster.privateIps
      }
    }
  }

  static Iterable<Integer> getPrivateAddresses( NetworkConfiguration configuration, String clusterName ) {
    Lists.newArrayList( NetworkConfigurations.iterateRanges( NetworkConfigurations.getPrivateAddressRanges( configuration, clusterName ) ) )
  }

  static Iterable<Integer> getPrivateAddresses( String clusterName ) {
    Optional<NetworkConfiguration> configuration = NetworkConfigurations.networkConfiguration
    if ( !configuration.present ) {
      throw new IllegalStateException( "Networking configuration not found for cluster '${clusterName}'" )
    }
    NetworkConfigurations.getPrivateAddresses( configuration.get( ), clusterName )
  }

  static NetworkConfiguration parse( final String configuration ) throws NetworkConfigurationException {
    final ObjectMapper mapper = new ObjectMapper( )
    mapper.setPropertyNamingStrategy( new UpperCamelPropertyNamingStrategy( ) )
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
    mapper.readValue( mapper.valueToTree( configuration ), NetworkConfiguration )
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
    clusters.collect{ Cluster cluster -> cluster.name }.with{ Collection<String> names ->
      clusters.addAll( ((Iterable) clusterNames).findResults{ String requiredName ->
        names.contains( requiredName ) ?
            null:
            new Cluster( name: requiredName )
      } )
    }

    // Populate values at cluster level
    clusters.each{ Cluster cluster ->  cluster.with{
      if ( !macPrefix ) {
        macPrefix = configuration.macPrefix
      }

      if ( ( subnet == null || subnet && subnet.name && !subnet.subnet ) && name ) {
        subnet = NetworkConfigurations.getSubnetForCluster( configuration, name )
      }

      // Groovy gets confused here so we use the "cluster." prefix for the privateIps property
      if ( cluster.privateIps == null && configuration.privateIps ) {
        cluster.privateIps = configuration.privateIps
      } else if ( !cluster.privateIps && subnet ) {
        cluster.privateIps = subnet.with{ IPRange.fromSubnet( subnet, netmask ).split( gateway ).collect{ IPRange range -> range.toString( ) } }
      }

      void
    } }

    // Remove any global subnets that are used by clusters
    Collection<String> clusterSubnetsAndNetmasks = clusters.collect{ Cluster cluster ->
      "${cluster?.subnet?.subnet}/${cluster?.subnet?.netmask}".toString( )
    }
    configuration.subnets?.removeAll{ Subnet subnet ->
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
   * <p>This method currently validates that:</p>
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
        validateIPsForSubnet( configuration.subnets[0], configuration.privateIps )
      }
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

  @PackageScope
  static Properties loadEucalyptusConf( ) {
    Properties properties = new Properties()
    File eucalyptusConf = new File( "${System.getenv('EUCALYPTUS')}/etc/eucalyptus/eucalyptus.conf" )
    if ( eucalyptusConf.canRead( ) && eucalyptusConf.isFile( ) ) {
      eucalyptusConf.newInputStream().withStream{ InputStream input ->
        properties.load( input )
      }
    }
    properties
  }

  @PackageScope
  static Optional<NetworkConfiguration> buildNetworkConfigurationFromProperties(
      final List<String> primaryInstanceDnsServers,
      final Map<String,String> properties
  ) {
    Optional<NetworkConfiguration> configuration = Optional.absent( )
    if ( 'EDGE' == getTrimmedDequotedProperty( properties, 'VNET_MODE' ) ) {
      configuration = Optional.of( new NetworkConfiguration(
          instanceDnsDomain: getTrimmedDequotedProperty( properties, "VNET_DOMAINNAME" ),
          instanceDnsServers: primaryInstanceDnsServers + [ getTrimmedDequotedProperty( properties, "VNET_DNS" ) ] as List<String>,
          publicIps: getFilteredDequotedPropertyList(  properties, 'VNET_PUBLICIPS', IPRange.&isIPRange ),
          privateIps: getFilteredDequotedPropertyList(  properties, 'VNET_PRIVATEIPS', IPRange.&isIPRange ),
          subnets: [
              new Subnet(
                  subnet: getTrimmedDequotedProperty( properties, "VNET_SUBNET" ),
                  netmask: getTrimmedDequotedProperty( properties, "VNET_NETMASK" ) ,
                  gateway: getTrimmedDequotedProperty( properties, "VNET_ROUTER" )
              )
          ]
      ) )
    }
    configuration
  }

  private static String getTrimmedDequotedProperty( Map<String,String> properties, String name ) {
    properties.get( name, '' ).trim( ).with{
      startsWith('"') && endsWith('"') ? substring( 1, length() - 1 ) : toString( )
    }.trim( )?:null
  }

  private static List<String> getFilteredDequotedPropertyList( Map<String,String> properties, String name, Closure filter ) {
    final Splitter splitter = Splitter.on( ' ' ).trimResults( ).omitEmptyStrings( )
    Lists.newArrayList( splitter.split( getTrimmedDequotedProperty( properties, name )?:'' ) )
        .findAll( filter ) as List<String>
  }

  private static Iterable<Integer> iterateRanges( Iterable<String> rangeIterable ) {
    Iterables.concat( Optional.presentInstances( Iterables.transform( rangeIterable, IPRange.parse( ) ) ) )
  }

  private static Iterable<String> iterateRangesAsString( Iterable<String> rangeIterable ) {
    Iterables.transform( iterateRanges( rangeIterable ), PrivateAddresses.fromInteger( ) )
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
      if ( Hosts.isCoordinator( ) && !Bootstrap.isShuttingDown( ) && !Databases.isVolatile( ) ) {
        try {
          Optional<NetworkConfiguration> configurationOptional = NetworkConfigurations.networkConfigurationFromProperty
          if ( !configurationOptional.isPresent( ) ) {
            EdgeNetworking.configured = false
            if ( EdgeNetworking.isEnabled( ) ) { // Configure from environment if possible
              configurationOptional = NetworkConfigurations.networkConfigurationFromEnvironment
            }
          } else {
            EdgeNetworking.configured = true
          }
          if ( EdgeNetworking.isEnabled( ) ) {
            Iterables.all(
                configurationOptional.or( NetworkConfigurations.networkConfigurationFromEnvironmentSupplier.get( ) ).asSet( ),
                Entities.asTransaction( ClusterConfiguration.class, { NetworkConfiguration networkConfiguration ->
                  NetworkConfigurations.process( networkConfiguration )
                  true
                } as Predicate<NetworkConfiguration> ) )
          }
        } catch ( e ) {
          logger.error( "Error updating network configuration", e )
        }
      }
    }
  }
}
