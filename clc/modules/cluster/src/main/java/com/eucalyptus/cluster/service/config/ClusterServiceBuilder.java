/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cluster.service.config;

import java.util.NoSuchElementException;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.ClusterRegistry;
import com.eucalyptus.cluster.service.ClusterServiceId;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.annotation.ComponentPart;

/**
 *
 */
@ComponentPart( ClusterServiceId.class )
@Handles( {
    RegisterClusterServiceType.class,
    DeregisterClusterServiceType.class,
    DescribeClusterServicesType.class,
    ModifyClusterServiceAttributeType.class
} )
public class ClusterServiceBuilder extends AbstractServiceBuilder<ClusterServiceConfiguration> {

  @Override
  public ComponentId getComponentId() {
    return ComponentIds.lookup( ClusterServiceId.class );
  }

  @Override
  public ClusterServiceConfiguration newInstance( final String partition, final String name, final String host, final Integer port ) {
    return new ClusterServiceConfiguration( partition, name, host, port );
  }

  @Override
  public ClusterServiceConfiguration newInstance() {
    return new ClusterServiceConfiguration( );
  }


  @Override
  public void fireStart( final ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( !registry( ).contains( config.getName( ) ) ) {
      final Cluster newCluster = new Cluster( config );
      newCluster.start( );
    }
  }

  @Override
  public void fireStop( final ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      registry( ).lookupDisabled( config.getName( ) ).stop( );
    } catch ( final NoSuchElementException ignore ) {
    }
  }

  @Override
  public void fireEnable( final ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      registry( ).lookupDisabled( config.getName( ) ).enable( );
    } catch ( final NoSuchElementException ignore ) {
    }
  }

  @Override
  public void fireDisable( final ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      registry( ).lookup( config.getName( ) ).disable( );
    } catch ( final NoSuchElementException ignore ) {
    }
  }

  @Override
  public void fireCheck( final ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      registry( ).lookup( config.getName( ) ).enable( );
    } catch ( final NoSuchElementException e ) {
      try {
        registry( ).lookupDisabled( config.getName( ) ).enable( );
      } catch ( final NoSuchElementException ignore ) {
      }
    }
  }

  private ClusterRegistry registry() {
    return ClusterRegistry.getInstance( );
  }
}