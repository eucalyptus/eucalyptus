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
package com.eucalyptus.network.config;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ValidationUtils;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.network.DispatchingNetworkingService;
import com.eucalyptus.network.IPRange;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.network.NetworkMode;
import com.eucalyptus.network.PrivateAddresses;
import com.eucalyptus.network.config.NetworkConfigurationApi.Cluster;
import com.eucalyptus.network.config.NetworkConfigurationApi.Subnet;
import com.eucalyptus.network.config.NetworkConfigurationApi.NetworkConfiguration;
import com.eucalyptus.network.config.NetworkConfigurationApi.EdgeSubnet;
import com.eucalyptus.network.config.NetworkConfigurationApi.NetworkConfigurationValidator;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Pair;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import io.vavr.collection.Array;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

/**
 *
 */
@SuppressWarnings( { "UnnecessaryQualifiedReference", "Guava", "StaticPseudoFunctionalStyleMethod" } )
public class NetworkConfigurations {

  private static final Logger logger = Logger.getLogger( NetworkConfigurations.class );
  private static final boolean validateConfiguration =
      Boolean.valueOf( System.getProperty( "com.eucalyptus.network.config.validateNetworkConfiguration", "true" ) );
  private static final AtomicReference<Supplier<Void>> managedFaultSupplier =
      new AtomicReference<>( Suppliers.memoize( managedFaultSupplier( ) ) );

  public static Option<NetworkConfiguration> getNetworkConfiguration( ) {
    return NetworkConfigurations.getNetworkConfigurationFromProperty( );
  }

  @SuppressWarnings( "WeakerAccess" )
  public static Option<NetworkConfiguration> getNetworkConfigurationFromProperty( ) {
    Option<NetworkConfiguration> configuration = Option.none( );
    String configurationText = NetworkGroups.NETWORK_CONFIGURATION;
    if ( !Strings.isNullOrEmpty( configurationText ) ) {
      try {
        Option<NetworkConfiguration> parsedConfiguration = Option.of( parse( configurationText ) );
        if ( parsedConfiguration.get( ).managedSubnet( ).isDefined( ) ) {
          managedFaultSupplier.get( ).get( );
        } else {
          managedFaultSupplier.set( Suppliers.memoize( managedFaultSupplier( ) ) );
          configuration = parsedConfiguration;
        }
      } catch ( NetworkConfigurationException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }

    return configuration;
  }

  public static List<String> loadSystemNameservers( final List<String> fallback ) {
    final String dnsServersProperty = SystemConfiguration.getSystemConfiguration( ).getNameserverAddress( );
    final List<String> dnsServers = Lists.newArrayList( Splitter.on( "," ).omitEmptyStrings( ).trimResults( ).split( dnsServersProperty ) );
    return "127.0.0.1".equals( dnsServersProperty ) || dnsServers.isEmpty( ) ? fallback : dnsServers;
  }

  static void process( final NetworkConfiguration networkConfiguration ) {
    Addresses.getInstance( ).update( iterateRangesAsString( networkConfiguration.publicIps( ) ) );
  }

  @SuppressWarnings( "WeakerAccess" )
  @Nullable
  public static EdgeSubnet getSubnetForCluster( NetworkConfiguration configuration, String clusterName ) {
    Array<Cluster> clusters = configuration.clusters( );
    Array<EdgeSubnet> subnets = configuration.subnets( );
    EdgeSubnet defaultSubnet = 1 == subnets.size( ) ? subnets.get( 0 ) : null;
    Cluster cluster = null;
    for ( Cluster current : clusters ) {
      if ( Objects.equals( clusterName, current.name( ).getOrNull( ) ) ) {
        cluster = current;
        break;
      }
    }

    EdgeSubnet clusterSubnetFallback = defaultSubnet;
    if ( cluster != null && cluster.subnet().isDefined() && cluster.subnet( ).get( ).name( ).isDefined( ) ) {
      for ( EdgeSubnet current : subnets ) {
        if ( current.name( ).orElse( current.subnet( ) ).get( ).equals( cluster.subnet( ).get( ).name( ).get( ) ) ) {
          clusterSubnetFallback = current;
          break;
        }
      }
    }

    if ( clusterSubnetFallback == null ) {
      clusterSubnetFallback = ImmutableNetworkConfigurationApi.EdgeSubnet.builder( ).o( );
    }

    EdgeSubnet clusterSubnet = cluster != null ? cluster.subnet( ).getOrElse( clusterSubnetFallback ) : clusterSubnetFallback;
    String name = firstDefined( clusterSubnet.name( ), clusterSubnet.subnet( ), clusterSubnetFallback.name( ), clusterSubnetFallback.subnet( ) );
    String subnet = firstDefined( clusterSubnet.subnet( ), clusterSubnetFallback.subnet( ) );
    String netmask = firstDefined( clusterSubnet.netmask( ), clusterSubnetFallback.netmask( ) );
    String gateway = firstDefined( clusterSubnet.gateway( ), clusterSubnetFallback.gateway( ) );
    return subnet != null && netmask != null && gateway != null ?
        ImmutableNetworkConfigurationApi.EdgeSubnet.builder()
            .name( Option.of( name ) )
            .setValueSubnet( subnet )
            .setValueNetmask( netmask )
            .setValueGateway( gateway )
            .o( ) :
        null;
  }

  @Nullable
  @SafeVarargs
  private static <T> T firstDefined( final Option<T>... ts ) {
    T result = null;
    if ( ts != null ) {
      for ( Option<T> t : ts ) {
        if ( t != null && t.isDefined( ) ) {
          result = t.get( );
          break;
        }
      }
    }

    return result;
  }

  @SuppressWarnings( "WeakerAccess" )
  public static Array<String> getPrivateAddressRanges( NetworkConfiguration configuration, final String clusterName ) {
    Array<String> addressRanges = Array.empty( );
    NetworkConfiguration exploded = NetworkConfigurations.explode( configuration, Lists.newArrayList( clusterName ) );
    for ( Cluster cluster : exploded.clusters( ) ) {
      if ( Objects.equals( cluster.name( ).getOrNull(), clusterName ) ) {
        if ( !cluster.subnet( ).isDefined( ) ) {
          // We check the subnet since if this is not configured the
          // request will fail on the back end
          throw new IllegalStateException( "Networking configuration not found for cluster \'" + clusterName + "\'" );
        }

        addressRanges = cluster.privateIps( );
        break;
      }
    }

    return addressRanges;
  }

  public static String getMacPrefix( ) {
    return getMacPrefix( NetworkConfigurations.getNetworkConfiguration( ) );
  }

  @SuppressWarnings( "OptionalUsedAsFieldOrParameterType" )
  public static String getMacPrefix( final Option<NetworkConfiguration> configuration ) {
    String macPrefix = VmInstances.MAC_PREFIX;
    if ( configuration.isDefined( ) ) {
      final NetworkConfiguration exploded = explode( configuration.get( ) );
      if ( 1 == exploded.clusters( ).size( ) && exploded.clusters( ).get( 0 ).macPrefix( ).isDefined( ) ) {
        macPrefix = exploded.clusters( ).get( 0 ).macPrefix( ).get( );
      } else if ( exploded.macPrefix( ).isDefined( ) ) {
        macPrefix = exploded.macPrefix( ).get( );
      }
    }
    return macPrefix;
  }

  public static Pair<Iterable<Integer>, Integer> getPrivateAddresses( NetworkConfiguration configuration, String clusterName ) {
    return NetworkConfigurations.iterateRanges( NetworkConfigurations.getPrivateAddressRanges( configuration, clusterName ) );
  }

  public static Pair<Iterable<Integer>, Integer> getPrivateAddresses( final String clusterName ) {
    Option<NetworkConfiguration> configuration = NetworkConfigurations.getNetworkConfiguration( );
    if ( !configuration.isDefined( ) ) {
      throw new IllegalStateException( "Networking configuration not found for cluster \'" + clusterName + "\'" );
    }

    return NetworkConfigurations.getPrivateAddresses( configuration.get( ), clusterName );
  }

  public static NetworkConfiguration parse( final String configuration ) throws NetworkConfigurationException {
    final NetworkConfiguration networkConfiguration;
    try {
      networkConfiguration = NetworkConfigurationApi.parse( configuration );
    } catch ( IOException e ) {
      throw new NetworkConfigurationException( e.getMessage( ) );
    }

    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult( networkConfiguration, "NetworkConfiguration" );
    ValidationUtils.invokeValidator( new NetworkConfigurationValidator( errors ), networkConfiguration, errors );
    if ( validateConfiguration && errors.hasErrors( ) ) {
      MessageSource source = new StaticMessageSource( );// default messages will be used
      throw new NetworkConfigurationException( source.getMessage( errors.getAllErrors( ).get( 0 ), Locale.getDefault( ) ) );
    }

    return networkConfiguration;
  }

  @SuppressWarnings( "WeakerAccess" )
  public static NetworkConfiguration explode( final NetworkConfiguration networkConfiguration ) {
    return explode( networkConfiguration, Lists.newArrayList( ) );
  }

  /**
   * Explode the configuration by populating all values at the cluster level
   * including filling in default values where possible.
   *
   * @param configuration The configuration to explode.
   * @param clusterNames  The required clusters in the result
   * @return The exploded configuration.
   */
  public static NetworkConfiguration explode( final NetworkConfiguration configuration, final Iterable<String> clusterNames ) {
    Array<Cluster> clusters = configuration.clusters( );
    Array<EdgeSubnet> subnets = configuration.subnets( );

    // Ensure required clusters exist
    List<String> existingClusterNames = Lists.newArrayList( );
    for ( Cluster cluster : clusters ) {
      existingClusterNames.add( cluster.name( ).get( ) );
    }

    for ( final String clusterName : clusterNames ) {
      if ( !existingClusterNames.contains( clusterName ) ) {
        clusters = clusters.append( ImmutableNetworkConfigurationApi.Cluster.builder( ).setValueName( clusterName ).o( ) );
      }
    }

    // Populate values at cluster level
    for ( Cluster cluster : clusters ) {
      if ( Strings.isNullOrEmpty( cluster.macPrefix( ).getOrNull( ) ) ) {
        clusters = clusters.replace(
            cluster,
            cluster = ImmutableNetworkConfigurationApi.Cluster.builder( ).from( cluster )
                .setValueMacPrefix( configuration.macPrefix( ).getOrElse( VmInstances.MAC_PREFIX ) ).o( ) );
      }

      // EDGE mode configuration
      EdgeSubnet subnet = cluster.subnet( ).getOrNull( );
      if ( !configuration.mode( ).isDefined() || "EDGE".equals( configuration.mode( ).get( ) ) ) {
        // check / resolve reference by name
        if ( ( subnet == null || ( subnet.name( ).isDefined() && subnet.subnet( ).isEmpty() ) ) && cluster.name( ).isDefined( ) ) {
          clusters = clusters.replace(
              cluster,
              cluster = ImmutableNetworkConfigurationApi.Cluster.builder( ).from( cluster )
                  .subnet( Option.of( subnet = NetworkConfigurations.getSubnetForCluster( configuration, cluster.name( ).get( ) ) ) )
                  .o() );
        }
      }


      Array<String> privateIps = Array.empty( );
      if ( cluster.privateIps( ).isEmpty( ) && !configuration.privateIps( ).isEmpty( ) ) {
        privateIps = configuration.privateIps( );
      } else if ( cluster.privateIps( ).isEmpty( ) && subnet != null ) {
        if ( !subnet.gateway( ).isEmpty( ) ) {
          for ( IPRange range : IPRange.fromSubnet( subnet.subnet( ).get( ), subnet.netmask( ).get( ) ).split( subnet.gateway( ).get( ) ) ) {
            privateIps = privateIps.append( range.toString( ) );
          }
        }
      }
      if ( !privateIps.isEmpty( ) ) {
        clusters = clusters.replace(
            cluster,
            ImmutableNetworkConfigurationApi.Cluster.builder( ).from( cluster )
                .privateIps( privateIps )
                .o() );
      }
    }

    // Remove any global subnets that are used by clusters
    CompatFunction<EdgeSubnet, String> edgeToString = edgeSubnet ->
            ( ( edgeSubnet == null ? "-" : edgeSubnet.subnet( ).getOrElse( "-" ) ) +
            "/" +
            ( edgeSubnet == null ? "-" : edgeSubnet.netmask( ).getOrElse( "-" ) ) );
    Collection<String> clusterSubnetsAndNetmasks = Sets.newHashSet( );
    for ( Cluster current : clusters ) {
      if ( current.subnet( ).isDefined( ) ) {
        clusterSubnetsAndNetmasks.add( edgeToString.apply( current.subnet().get( ) ) );
      }
    }

    for ( EdgeSubnet current : subnets ) {
      if ( clusterSubnetsAndNetmasks.contains( edgeToString.apply( current ) ) ) {
        subnets = subnets.remove( current );
      }
    }

    return ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
        .from( configuration )
        .clusters( clusters )
        .subnets( subnets )
        .o( );
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
   * <li>Exactly one subnet (with valid IPs) is configured if there are no clusters specified</li>
   * <li>Each cluster has valid private IPs for its subnet.</li>
   * </ul>
   *
   * <p>Lower level validation is performed when parsing, this method will only
   * perform the semantic validation described above.</p>
   *
   * @see #explode(NetworkConfiguration)
   */
  public static void validate( final NetworkConfiguration configuration ) throws NetworkConfigurationException {
    if ( configuration == null ) return;

    if ( "EDGE".equals( configuration.mode( ).getOrElse( "EDGE" ) ) ) {
      if ( !configuration.clusters( ).isEmpty( ) ) {
        for ( Cluster cluster : configuration.clusters( ) ) {
          if ( cluster.subnet( ).isDefined( ) && !cluster.privateIps( ).isEmpty( ) ) {
            validateIPsForSubnet( cluster.subnet( ).get( ), cluster.privateIps( ), " for cluster " + cluster.name( ).getOrElse( "-" ) );
          }
        }
      } else {
        if ( configuration.subnets( ).size( ) != 1 ) {
          throw new NetworkConfigurationException( "A single subnet must be configured when clusters are not specified." );
        }

        validateIPsForSubnet( configuration.subnets( ).get( 0 ), configuration.privateIps( ) , "" );
      }

    } else if ( "VPCMIDO".equals( configuration.mode( ).getOrElse( "EDGE" ) ) ) {
      if ( !configuration.clusters( ).isEmpty( ) ) {
        for ( Cluster cluster : configuration.clusters( ) ) {
          if ( !cluster.privateIps( ).isEmpty( ) ) {
            throw new NetworkConfigurationException( "Private IP configuration not permitted for VPCMIDO." );
          }

          if ( cluster.subnet( ).isDefined() ) {
            throw new NetworkConfigurationException( "Subnet configuration not permitted for VPCMIDO." );
          }
        }
      }

      if ( !configuration.privateIps( ).isEmpty( ) ) {
        throw new NetworkConfigurationException( "Private IP configuration not permitted for VPCMIDO." );
      }

      if ( !configuration.subnets( ).isEmpty( ) ) {
        throw new NetworkConfigurationException( "Subnet configuration not permitted for VPCMIDO." );
      }
    }
  }

  private static void validateIPsForSubnet( final Subnet net, final Array<String> ipRanges, final String desc ) throws NetworkConfigurationException {
    String subnet = net.subnet( ).get( );
    String netmask = net.netmask( ).get( );
    IPRange subnetRange = IPRange.fromSubnet( subnet, netmask );
    if ( ipRanges != null ) {
      for ( IPRange range : Stream.ofAll( ipRanges ).flatMap( IPRange.optParse( ) ) ) {
        if ( !subnetRange.contains( range ) ) {
          throw new NetworkConfigurationException( "Private IP range " + String.valueOf( range ) + " not valid for subnet/netmask " + subnet + "/" + netmask + desc );
        }
      }
    }
  }

  private static Pair<Iterable<Integer>, Integer> iterateRanges( Iterable<String> rangeIterable ) {
    final Array<IPRange> ranges = Array.ofAll( rangeIterable ).flatMap( IPRange.optParse( ) );
    int rangesSize = 0;
    for ( IPRange range : ranges ) {
      rangesSize = rangesSize + range.size( );
    }
    return Pair.pair( Iterables.concat( ranges ), rangesSize );
  }

  private static Iterable<String> iterateRangesAsString( Iterable<String> rangeIterable ) {
    return Iterables.transform( iterateRanges( rangeIterable ).getLeft( ), PrivateAddresses.fromInteger( ) );
  }

  private static Supplier<Void> managedFaultSupplier( ) {
    return () -> {
        logger.error( "Managed network modes not supported, network configuration ignored" );
        Faults.forComponent( Eucalyptus.class ).havingId( 1017 ).log( );
        return null;
    };
  }

  public static class NetworkConfigurationPropertyChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange( final ConfigurableProperty property, final String newValue ) throws ConfigurablePropertyException {
      if ( !Strings.isNullOrEmpty( newValue ) ) {
        try {
          validate( explode( parse( newValue ) ) );
        } catch ( Exception e ) {
          throw new ConfigurablePropertyException( e.getMessage( ), e );
        }
      }
    }
  }

  @SuppressWarnings( "unused" )
  public static class NetworkConfigurationEventListener implements EventListener<ClockTick> {

    public static void register( ) {
      Listeners.register( ClockTick.class, new NetworkConfigurationEventListener( ) );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      if ( !Bootstrap.isShuttingDown( ) && !Databases.isVolatile( ) ) {
        try {
          Option<NetworkConfiguration> configurationOptional = NetworkConfigurations.getNetworkConfiguration( );
          if ( configurationOptional.isDefined( ) ) {
            DispatchingNetworkingService.updateNetworkService( NetworkMode.fromString( configurationOptional.get( ).mode( ).getOrNull( ), NetworkMode.EDGE ) );
            if ( Hosts.isCoordinator( ) ) {
              NetworkConfiguration networkConfiguration = configurationOptional.get( );
              NetworkConfigurations.process( networkConfiguration );
            }
          }
        } catch ( Exception e ) {
          logger.error( "Error updating network configuration", e );
        }
      }
    }
  }
}
