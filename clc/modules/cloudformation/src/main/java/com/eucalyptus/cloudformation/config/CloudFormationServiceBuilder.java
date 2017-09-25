/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cloudformation.config;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cloudformation.common.CloudFormation;
import com.eucalyptus.cloudformation.workflow.WorkflowClientManager;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 *
 */
@ComponentPart( CloudFormation.class )
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
    if ( Bootstrap.isShuttingDown( ) || ( config.isVmLocal( ) && noOtherEnabled( config ) ) ) {
      pendingStopTimestamp.compareAndSet( 0, System.currentTimeMillis( ) );
      if ( Bootstrap.isShuttingDown( ) ) {
        stopWorkflowClient( pendingStopTimestamp.get( ) );
      }
    }
  }

  @Override
  public void fireCheck( ServiceConfiguration config ) { }

  @SuppressWarnings( "unchecked" )
  private boolean noOtherEnabled( final ServiceConfiguration config ) {
    return Components.services( CloudFormation.class )
        .filter( ServiceConfigurations.filterHostLocal( ) )
        .filter( ServiceConfigurations.filterEnabled( ) )
        .filter( Predicate.isEqual( config ).negate( ) )
        .isEmpty( );
  }

  private static void stopWorkflowClient( final long timestampMatch ) {
    synchronized ( startStopSync ) {
      if ( pendingStopTimestamp.compareAndSet( timestampMatch, 0 ) ) {
        try {
          logger.info( "Stopping cloudformation workflow client" );
          WorkflowClientManager.stop( );
        } catch ( Exception e ) {
          logger.error( "Error stopping cloudformation workflow client", e );
        }
      }
    }
  }

  public static class CloudFormationWorkflowStopEventListener implements EventListener<ClockTick> {
    public static void register( ) {
      Listeners.register( ClockTick.class, new CloudFormationWorkflowStopEventListener( ) );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      final long stopRequestedTime = pendingStopTimestamp.get( );
      if ( stopRequestedTime > 0 && stopRequestedTime + PENDING_STOP_TIMEOUT < System.currentTimeMillis( ) ) {
        stopWorkflowClient( stopRequestedTime );
      }
    }
  }
}
