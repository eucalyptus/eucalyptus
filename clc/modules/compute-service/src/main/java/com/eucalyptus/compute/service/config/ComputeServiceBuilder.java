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
package com.eucalyptus.compute.service.config;

import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.config.DeregisterComputeType;
import com.eucalyptus.compute.common.config.DescribeComputeType;
import com.eucalyptus.compute.common.config.ModifyComputeAttributeType;
import com.eucalyptus.compute.common.config.RegisterComputeType;

/**
 *
 */
@ComponentPart( Compute.class )
@Handles( {
    DeregisterComputeType.class,
    DescribeComputeType.class,
    ModifyComputeAttributeType.class,
    RegisterComputeType.class,
} )
public class ComputeServiceBuilder extends AbstractServiceBuilder<ComputeConfiguration> {

  @Override
  public ComputeConfiguration newInstance( ) {
    return new ComputeConfiguration( );
  }

  @Override
  public ComputeConfiguration newInstance( String partition, String name, String host, Integer port ) {
    return new ComputeConfiguration( name, host, port );
  }

  @Override
  public ComponentId getComponentId( ) {
    return ComponentIds.lookup( Compute.class );
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
