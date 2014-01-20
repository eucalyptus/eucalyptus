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
import com.eucalyptus.address.AddressingDispatcher
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
import com.eucalyptus.network.DispatchingNetworkingService
import com.eucalyptus.network.NetworkGroups
import com.eucalyptus.util.Exceptions
import com.google.common.base.Optional
import com.google.common.base.Predicate
import com.google.common.base.Strings
import com.google.common.collect.Iterables
import groovy.transform.CompileStatic
import org.apache.log4j.Logger
import org.codehaus.jackson.JsonProcessingException
import org.codehaus.jackson.map.ObjectMapper
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.ValidationUtils

import javax.persistence.EntityTransaction

/**
 *
 */
@CompileStatic
class NetworkConfigurations {
  private static final Logger logger = Logger.getLogger( NetworkConfigurations )

  static Optional<NetworkConfiguration> getNetworkConfiguration( ) {
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

  /**
   * TODO:STEVE: call this on property value change or cluster registration change
   */
  static void process( final NetworkConfiguration networkConfiguration ) {
    DispatchingNetworkingService.updateNetworkMode( 'EDGE' );
    AddressingDispatcher.enable( AddressingDispatcher.Dispatcher.SHORTCUT )
    Addresses.addressManager.update( networkConfiguration.publicIps ) //TODO:STEVE: process IP ranges
    Entities.transaction( ClusterConfiguration.class ) { EntityTransaction db ->
      Components.lookup(ClusterController.class).services().each { ClusterConfiguration config ->
        networkConfiguration.clusters.find{ Cluster cluster -> cluster.name == config.name }?.with{
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
    //TODO:STEVE: Expand IP address ranges, use top level configuration as default?
    NetworkConfigurations.networkConfiguration.orNull()?.clusters?.find{ Cluster cluster -> cluster.name == clusterName }?.privateIps ?: []
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
          Iterables.all(
              NetworkConfigurations.getNetworkConfiguration( ).asSet( ),
              Entities.asTransaction( ClusterConfiguration.class, { NetworkConfiguration networkConfiguration ->
                NetworkConfigurations.process( networkConfiguration )
                true
              } as Predicate<NetworkConfiguration> ) )
        } catch ( e ) {
          logger.error( "Error updating network configuration", e )
        }
      }
    }
  }
}
