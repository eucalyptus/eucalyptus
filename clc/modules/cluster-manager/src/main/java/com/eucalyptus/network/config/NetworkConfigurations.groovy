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
import com.eucalyptus.network.NetworkGroups
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
import com.google.common.net.InetAddresses
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.apache.log4j.Logger
import org.codehaus.jackson.JsonProcessingException
import org.codehaus.jackson.map.ObjectMapper
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.ValidationUtils

import javax.persistence.EntityTransaction
import java.util.concurrent.TimeUnit

/**
 *
 */
@CompileStatic
class NetworkConfigurations {
  private static final Logger logger = Logger.getLogger( NetworkConfigurations )
  private static final Supplier<Optional<NetworkConfiguration>> networkConfigurationFromEnvironmentSupplier =
      Suppliers.memoizeWithExpiration(
          NetworkConfigurations.&loadNetworkConfigurationFromEnvironment as Supplier<Optional<NetworkConfiguration>>,
          5,
          TimeUnit.MINUTES
      )

  @SuppressWarnings("UnnecessaryQualifiedReference")
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
  static Optional<NetworkConfiguration> loadNetworkConfigurationFromEnvironment( ) {
    buildNetworkConfigurationFromProperties( loadEucalyptusConf( ).collectEntries( Maps.<String,String>newHashMap( ) ){
      Object key, Object value -> [ String.valueOf(key), String.valueOf(value) ]
    } )
  }

  @PackageScope
  static void process( final NetworkConfiguration networkConfiguration ) {
    Addresses.addressManager.update( networkConfiguration.publicIps ) //TODO:STEVE: process IP ranges
    Entities.transaction( ClusterConfiguration.class ) { EntityTransaction db ->
      Components.lookup(ClusterController.class).services().each { ClusterConfiguration config ->
        networkConfiguration?.clusters?.find{ Cluster cluster -> cluster.name == config.name }?:new Cluster().with{
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
            defaultSubnet = networkConfiguration.subnets.find{ Subnet s -> s.name?:s.subnet == subnet.name }
          } else if ( !subnet ) {
            defaultSubnet = networkConfiguration.subnets.getAt(0) // must be only one
          }

          clusterConfiguration.vnetSubnet = subnet?.subnet?:defaultSubnet?.subnet
          clusterConfiguration.vnetNetmask = subnet?.netmask?:defaultSubnet?.netmask
        }
      }
      db.commit( );
    }
  }

  @SuppressWarnings("UnnecessaryQualifiedReference")
  static Collection<String> getPrivateAddresses( final String clusterName ) {
    //TODO:STEVE: Expand IP address ranges?
    NetworkConfiguration networkConfiguration = NetworkConfigurations.networkConfiguration.orNull()
    networkConfiguration?.clusters?.find{ Cluster cluster -> cluster.name == clusterName }?.privateIps ?:
        networkConfiguration?.privateIps ?:
            [ ]
  }

  static NetworkConfiguration parse( final String configuration ) throws NetworkConfigurationException {
    final ObjectMapper mapper = new ObjectMapper( )
    mapper.setPropertyNamingStrategy( new UpperCamelPropertyNamingStrategy( ) )
    final NetworkConfiguration networkConfiguration
    try {
      networkConfiguration = mapper.readValue( configuration, NetworkConfiguration.class )
    } catch ( JsonProcessingException e ) {
      throw new NetworkConfigurationException( e.getMessage( ) )
    }
    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult( networkConfiguration, "NetworkConfiguration");
    ValidationUtils.invokeValidator( new NetworkConfigurationValidator(errors), networkConfiguration, errors )
    if ( errors.getGlobalErrorCount( ) > 0 ) {
      throw new NetworkConfigurationException( errors.getGlobalErrors( ).get( 0 ).toString() )
    }
    networkConfiguration
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
  static Optional<NetworkConfiguration> buildNetworkConfigurationFromProperties( final Map<String,String> properties ) {
    Optional<NetworkConfiguration> configuration = Optional.absent( )
    if ( 'EDGE' == getTrimmedDequotedProperty( properties, 'VNET_MODE' ) ) {
      configuration = Optional.of( new NetworkConfiguration(
          instanceDnsDomain: getTrimmedDequotedProperty( properties, "VNET_DOMAINNAME" ),
          instanceDnsServers: getTrimmedDequotedProperty( properties, "VNET_DNS" ),
          publicIps: getFilteredDequotedPropertyList(  properties, 'VNET_PUBLICIPS', InetAddresses.&isInetAddress ), // TODO:STEVE: process IP ranges
          privateIps: getFilteredDequotedPropertyList(  properties, 'VNET_PRIVATEIPS', InetAddresses.&isInetAddress ),
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

  static class NetworkConfigurationPropertyChangeListener implements PropertyChangeListener<String> {
    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    void fireChange( final ConfigurableProperty property,
                     final String newValue ) throws ConfigurablePropertyException {
      if ( !Strings.isNullOrEmpty( newValue ) ) {
        try {
          parse( newValue )
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

    @SuppressWarnings("UnnecessaryQualifiedReference")
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
