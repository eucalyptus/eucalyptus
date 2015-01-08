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
package com.eucalyptus.simpleworkflow.config;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.eucalyptus.simpleworkflow.common.config.DeregisterSimpleWorkflowType;
import com.eucalyptus.simpleworkflow.common.config.DescribeSimpleWorkflowType;
import com.eucalyptus.simpleworkflow.common.config.ModifySimpleWorkflowAttributeType;
import com.eucalyptus.simpleworkflow.common.config.RegisterSimpleWorkflowType;

/**
 *
 */
@ComponentPart( SimpleWorkflow.class )
@Handles( {
    DeregisterSimpleWorkflowType.class,
    DescribeSimpleWorkflowType.class,
    ModifySimpleWorkflowAttributeType.class,
    RegisterSimpleWorkflowType.class,
} )
public class SimpleWorkflowServiceBuilder extends AbstractServiceBuilder<SimpleWorkflowConfiguration> {
  private static final Logger LOG = Logger.getLogger( SimpleWorkflowServiceBuilder.class );

  @Override
  public SimpleWorkflowConfiguration newInstance( ) {
    return new SimpleWorkflowConfiguration( );
  }

  @Override
  public SimpleWorkflowConfiguration newInstance( String partition, String name, String host, Integer port ) {
    return new SimpleWorkflowConfiguration( partition, name, host, port );
  }

  @Override
  public ComponentId getComponentId( ) {
    return ComponentIds.lookup( SimpleWorkflow.class );
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
