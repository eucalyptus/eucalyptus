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
package com.eucalyptus.cloudformation.config;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.cloudformation.CloudFormation;
import com.eucalyptus.cloudformation.workflow.WorkflowClientManager;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.annotation.ComponentPart;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

/**
 *
 */
@ComponentPart( CloudFormation.class )
@Handles( {
    DeregisterCloudFormationType.class,
    DescribeCloudFormationType.class,
    ModifyCloudFormationAttributeType.class,
    RegisterCloudFormationType.class,
} )
public class CloudFormationServiceBuilder extends AbstractServiceBuilder<CloudFormationConfiguration> {

  private static final Logger logger = Logger.getLogger( CloudFormationServiceBuilder.class );

  @Override
  public CloudFormationConfiguration newInstance( ) {
    return new CloudFormationConfiguration( );
  }

  @Override
  public CloudFormationConfiguration newInstance( String partition, String name, String host, Integer port ) {
    return new CloudFormationConfiguration( partition, name, host, port );
  }

  @Override
  public ComponentId getComponentId( ) {
    return ComponentIds.lookup( CloudFormation.class );
  }

  @Override
  public void fireStart( ServiceConfiguration config ) { }

  @Override
  public void fireStop( ServiceConfiguration config ) { }

  @Override
  public void fireEnable( final ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( config.isVmLocal( ) && noOtherEnabled( config ) ) try {
      WorkflowClientManager.start( );
    } catch ( Exception e ) {
      throw new ServiceRegistrationException( "Error creating workflow client", e );
    }
  }

  @Override
  public void fireDisable( final ServiceConfiguration config ) {
    if ( config.isVmLocal( ) && noOtherEnabled( config ) ) try {
      WorkflowClientManager.stop( );
    } catch ( Exception e ) {
      logger.error( "Error stopping workflow client", e );
    }
  }

  @Override
  public void fireCheck( ServiceConfiguration config ) { }

  @SuppressWarnings( "unchecked" )
  private boolean noOtherEnabled( final ServiceConfiguration config ) {
    return Iterables.isEmpty( ServiceConfigurations.filter( CloudFormation.class, Predicates.and(
        ServiceConfigurations.filterHostLocal( ),
        ServiceConfigurations.filterEnabled( ),
        Predicates.not( Predicates.equalTo( config ) ) ) ) );
  }
}
