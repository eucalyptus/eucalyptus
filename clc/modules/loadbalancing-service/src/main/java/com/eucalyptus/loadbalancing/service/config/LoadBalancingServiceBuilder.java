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
package com.eucalyptus.loadbalancing.service.config;

import org.apache.log4j.Logger;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.balancing.common.config.DeregisterLoadBalancingType;
import com.eucalyptus.balancing.common.config.DescribeLoadBalancingType;
import com.eucalyptus.balancing.common.config.ModifyLoadBalancingAttributeType;
import com.eucalyptus.balancing.common.config.RegisterLoadBalancingType;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.annotation.ComponentPart;

/**
 *
 */
@ComponentPart( LoadBalancing.class )
@Handles( {
    DeregisterLoadBalancingType.class,
    DescribeLoadBalancingType.class,
    ModifyLoadBalancingAttributeType.class,
    RegisterLoadBalancingType.class,
} )
public class LoadBalancingServiceBuilder extends AbstractServiceBuilder<LoadBalancingConfiguration> {
  private static final Logger LOG = Logger.getLogger( LoadBalancingServiceBuilder.class );

  @Override
  public LoadBalancingConfiguration newInstance( ) {
    return new LoadBalancingConfiguration( );
  }

  @Override
  public LoadBalancingConfiguration newInstance( String partition, String name, String host, Integer port ) {
    return new LoadBalancingConfiguration( name, host, port );
  }

  @Override
  public ComponentId getComponentId( ) {
    return ComponentIds.lookup( LoadBalancing.class );
  }

  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException { }

  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException { }

  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException { }

  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException { }

  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException { }

}
