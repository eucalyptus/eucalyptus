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
package com.eucalyptus.cluster.service.fake;

import java.util.NoSuchElementException;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.cluster.common.internal.Cluster;
import com.eucalyptus.cluster.common.internal.ClusterRegistry;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.annotation.ComponentPart;

/**
 *
 */
@ComponentPart( FakeCluster.class )
@Handles( {
    RegisterFakeClusterType.class,
    DeregisterFakeClusterType.class,
    DescribeFakeClustersType.class,
    ModifyFakeClusterAttributeType.class
} )
public class FakeClusterServiceBuilder extends AbstractServiceBuilder<FakeClusterConfiguration> {

  @Override
  public ComponentId getComponentId( ) {
    return ComponentIds.lookup( FakeCluster.class );
  }

  @Override
  public FakeClusterConfiguration newInstance( final String partition, final String name, final String host, final Integer port ) {
    return new FakeClusterConfiguration( partition, name, host, port );
  }

  @Override
  public FakeClusterConfiguration newInstance( ) {
    return new FakeClusterConfiguration( );
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

  private ClusterRegistry registry( ) {
    return ClusterRegistry.getInstance( );
  }
}
