/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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

  private static final AtomicLong pendingStopTimestamp = new AtomicLong( 0 );
  private static final long PENDING_STOP_TIMEOUT = TimeUnit.MINUTES.toMillis( 1 );
  private static final Object startStopSync = new Object( );

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
    if ( config.isVmLocal( ) && noOtherEnabled( config ) ) {
      synchronized ( startStopSync ) {
        final long stopRequestedTime = pendingStopTimestamp.get( );
        if ( stopRequestedTime <= 0 || !pendingStopTimestamp.compareAndSet( stopRequestedTime, 0 ) ) {
          // if stop not requested or we cannot cancel the pending stop then start the client
          try {
            logger.info( "Starting cloudformation workflow client" );
            WorkflowClientManager.start( );
          } catch ( Exception e ) {
            throw new ServiceRegistrationException( "Error creating workflow client", e );
          }
        }
      }
    }
  }

  @Override
  public void fireDisable( final ServiceConfiguration config ) {
    if ( config.isVmLocal( ) && noOtherEnabled( config ) ) {
      pendingStopTimestamp.compareAndSet( 0, System.currentTimeMillis( ) );
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

  public static class CloudFormationWorkflowStopEventListener implements EventListener<ClockTick> {
    public static void register( ) {
      Listeners.register( ClockTick.class, new CloudFormationWorkflowStopEventListener( ) );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      final long stopRequestedTime = pendingStopTimestamp.get( );
      if ( stopRequestedTime > 0 && stopRequestedTime + PENDING_STOP_TIMEOUT < System.currentTimeMillis( ) ) {
        synchronized ( startStopSync ) {
          if ( pendingStopTimestamp.compareAndSet( stopRequestedTime, 0 ) ) {
            try {
              logger.info( "Stopping cloudformation workflow client" );
              WorkflowClientManager.stop( );
            } catch ( Exception e ) {
              logger.error( "Error stopping cloudformation workflow client", e );
            }
          }
        }
      }
    }
  }
}
