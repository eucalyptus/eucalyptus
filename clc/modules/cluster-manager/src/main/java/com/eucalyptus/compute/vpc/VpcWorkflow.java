/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.vpc;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.compute.ComputeException;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vpc.InternetGateway;
import com.eucalyptus.compute.common.internal.vpc.InternetGateways;
import com.eucalyptus.compute.common.internal.vpc.NatGateway;
import com.eucalyptus.compute.common.internal.vpc.NatGateways;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaces;
import com.eucalyptus.compute.common.internal.vpc.Route;
import com.eucalyptus.compute.common.internal.vpc.RouteTable;
import com.eucalyptus.compute.common.internal.vpc.RouteTables;
import com.eucalyptus.compute.common.internal.vpc.Subnet;
import com.eucalyptus.compute.common.internal.vpc.Vpc;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataException;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.compute.vpc.persist.PersistenceInternetGateways;
import com.eucalyptus.compute.vpc.persist.PersistenceNatGateways;
import com.eucalyptus.compute.vpc.persist.PersistenceNetworkInterfaces;
import com.eucalyptus.compute.vpc.persist.PersistenceRouteTables;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.PersistenceExceptions;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.event.SystemClock;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/**
 *
 */
public class VpcWorkflow {

  private static final Logger logger = Logger.getLogger( VpcWorkflow.class );

  private static final Set<RouteKey> routesToCheck = Sets.newConcurrentHashSet( );

  private final VpcInvalidator vpcInvalidator;
  private final InternetGateways internetGateways;
  private final NatGateways natGateways;
  private final NetworkInterfaces networkInterfaces;
  private final RouteTables routeTables;

  private final List<WorkflowTask> workflowTasks = ImmutableList.<WorkflowTask>builder()
      .add( new WorkflowTask(  10, "NatGateway.SetupNetworkInterface" ) { @Override void doWork( ) throws Exception { natGatewaySetupNetworkInterface( ); } } )
      .add( new WorkflowTask(  10, "NatGateway.SetupElasticIP"        ) { @Override void doWork( ) throws Exception { natGatewaySetupElasticIp( ); } } )
      .add( new WorkflowTask(  10, "NatGateway.Delete"                ) { @Override void doWork( ) throws Exception { natGatewayDelete( ); } } )
      .add( new WorkflowTask(  30, "NatGateway.FailureCleanup"        ) { @Override void doWork( ) throws Exception { natGatewayFailureCleanup( ); } } )
      .add( new WorkflowTask( 300, "NatGateway.Timeout"               ) { @Override void doWork( ) throws Exception { natGatewayTimeout( ); } } )
      .add( new WorkflowTask(  10, "Route.StateCheck"                 ) { @Override void doWork( ) throws Exception { routeStateCheck( ); } } )
      .build( );

  public VpcWorkflow(
      final VpcInvalidator vpcInvalidator,
      final InternetGateways internetGateways,
      final NatGateways natGateways,
      final NetworkInterfaces networkInterfaces,
      final RouteTables routeTables
  ) {
    this.vpcInvalidator = vpcInvalidator;
    this.internetGateways = internetGateways;
    this.natGateways = natGateways;
    this.networkInterfaces = networkInterfaces;
    this.routeTables = routeTables;
  }

  private void doWorkflow( ) {
    for ( final WorkflowTask workflowTask : workflowTasks ) {
      try {
        workflowTask.perhapsWork( );
      } catch ( Exception e ) {
        logger.error( e, e );
      }
    }
  }

  private List<String> listNatGatewayIds( NatGateway.State state ) {
    List<String> natGatewayIds = Collections.emptyList( );
    try ( final TransactionResource tx = Entities.transactionFor( NatGateway.class ) ) {
      natGatewayIds = natGateways.listByExample(
          NatGateway.exampleWithState( state ),
          Predicates.<NatGateway>alwaysTrue( ),
          CloudMetadatas.<NatGateway>toDisplayName( ) );
    } catch ( VpcMetadataException e ) {
      logger.error( "Error listing NAT gateways", e );
    }
    return natGatewayIds;
  }

  /**
   * Progress pending NAT gateway to available by setting up the network interface.
   *
   * This task also checks some pre-requisites (e.g. internet gateway)
   */
  private void natGatewaySetupNetworkInterface( ) {
    for ( final String natGatewayId : listNatGatewayIds( NatGateway.State.pending ) ) {
      boolean cleanup = false;
      try ( final TransactionResource tx = Entities.transactionFor( NatGateway.class ) ) {
        final NatGateway natGateway = natGateways.lookupByName( null, natGatewayId, Functions.<NatGateway>identity( ) );
        final Vpc vpc = natGateway.getVpc( );
        final Subnet subnet = natGateway.getSubnet( );
        final AccountFullName accountFullName = AccountFullName.getInstance( natGateway.getOwnerAccountNumber( ) );

        if ( natGateway.getNetworkInterface( ) == null ) try { // do work for NAT gateway
          logger.info( "Setting up network interface for pending NAT gateway " + natGatewayId );

          // verify that there is an internet gateway present
          try {
            internetGateways.lookupByVpc( accountFullName, vpc.getDisplayName( ), CloudMetadatas.toDisplayName( ) );
          } catch ( final VpcMetadataNotFoundException e ) {
            throw new ClientComputeException( "Gateway.NotAttached", "Internet gateway not found for VPC" );
          }

          networkInterfaces.save( NatGatewayHelper.createNetworkInterface( natGateway, subnet ) );
        } catch ( final ComputeException e ) { // NAT gateway creation failure
          cleanup = true;
          natGateway.markDeletion( );
          natGateway.setState( NatGateway.State.failed );
          natGateway.setFailureCode( e.getCode( ) );
          natGateway.setFailureMessage( e.getMessage( ) );
        }
        tx.commit( );
      } catch ( Exception e ) {
        if ( PersistenceExceptions.isStaleUpdate( e ) ) {
          logger.debug( "Conflict updating NAT gateway " + natGatewayId + " (will retry)" );
        } else {
          logger.error( "Error processing pending NAT gateway " + natGatewayId, e );
        }
      }
      if ( cleanup ) {
        natGatewayFailureCleanup( natGatewayId );
      }
    }
  }

  /**
   * Move pending NAT gateway to available by setting up the Elastic IP
   */
  private void natGatewaySetupElasticIp( ) {
    for ( final String natGatewayId : listNatGatewayIds( NatGateway.State.pending ) ) {
      boolean invalidate = false;
      boolean cleanup = false;
      try ( final TransactionResource tx = Entities.transactionFor( NatGateway.class ) ) {
        final NatGateway natGateway = natGateways.lookupByName( null, natGatewayId, Functions.<NatGateway>identity( ) );
        if ( natGateway.getNetworkInterface( ) != null &&
            natGateway.getPublicIpAddress( ) == null ) try { // do work for NAT gateway
          logger.info( "Setting up Elastic IP for pending NAT gateway " + natGatewayId );
          NatGatewayHelper.associatePublicAddress( natGateway );
          natGateway.setState( NatGateway.State.available );
          invalidate = true;
        } catch ( final ComputeException e ) { // NAT gateway creation failure
          cleanup = true;
          natGateway.markDeletion( );
          natGateway.setState( NatGateway.State.failed );
          natGateway.setFailureCode( e.getCode( ) );
          natGateway.setFailureMessage( e.getMessage( ) );
        }
        tx.commit( );
      } catch ( Exception e ) {
        if ( PersistenceExceptions.isStaleUpdate( e ) ) {
          logger.debug( "Conflict updating NAT gateway " + natGatewayId + " (will retry)" );
        } else {
          logger.error( "Error processing pending NAT gateway " + natGatewayId, e );
        }
        continue;
      }
      if ( invalidate ) {
        vpcInvalidator.invalidate( natGatewayId );
      }
      if ( cleanup ) {
        natGatewayFailureCleanup( natGatewayId );
      }
    }
  }

  /**
   * Release resources for failed NAT gateways
   */
  private void natGatewayFailureCleanup( ) {
    List<String> failedNatGatewayIds = Collections.emptyList( );
    try ( final TransactionResource tx = Entities.transactionFor( NatGateway.class ) ) {
      failedNatGatewayIds = natGateways.list(
          null,
          Restrictions.and(
              Example.create( NatGateway.exampleWithState( NatGateway.State.failed ) ),
              Restrictions.isNotNull( "networkInterfaceId" )
          ),
          Collections.<String, String>emptyMap( ),
          Predicates.<NatGateway>alwaysTrue( ),
          CloudMetadatas.<NatGateway>toDisplayName( )
      );
    } catch ( final Exception e ) {
      logger.error( "Error listing failed NAT gateways for cleanup", e );
    }

    failedNatGatewayIds.forEach( this::natGatewayFailureCleanup );
  }

  /**
   * Release resources for a failed NAT gateway
   */
  private void natGatewayFailureCleanup( final String natGatewayId ) {
    try ( final TransactionResource tx = Entities.transactionFor( NatGateway.class ) ) {
      final NatGateway natGateway = natGateways.lookupByName( null, natGatewayId, Functions.<NatGateway>identity( ) );
      releaseNatGatewayResources( natGateway );
      tx.commit( );
    } catch ( Exception e ) {
      if ( PersistenceExceptions.isStaleUpdate( e ) ) {
        logger.debug( "Conflict updating NAT gateway " + natGatewayId + " for cleanup (will retry)" );
      } else {
        logger.error( "Error cleaning up failed NAT gateway " + natGatewayId, e );
      }
    }
  }

  /**
   * Delete NAT gateways that have timed out in a terminal state (failed or deleted)
   */
  private void natGatewayTimeout( ) {
    List<String> timedOutNatGateways = Collections.emptyList( );
    try ( final TransactionResource tx = Entities.transactionFor( NatGateway.class ) ) {
      timedOutNatGateways = natGateways.list(
          null,
          Restrictions.and(
              Restrictions.or(
                  Example.create( NatGateway.exampleWithState( NatGateway.State.failed ) ),
                  Example.create( NatGateway.exampleWithState( NatGateway.State.deleted ) )
              ),
              Restrictions.lt( "lastUpdateTimestamp", new Date( System.currentTimeMillis( ) - NatGateways.EXPIRY_AGE ) )
          ),
          Collections.<String, String>emptyMap( ),
          Predicates.<NatGateway>alwaysTrue( ),
          CloudMetadatas.<NatGateway>toDisplayName( )
      );
    } catch ( final Exception e ) {
      logger.error( "Error listing timed out NAT gateways", e );
    }

    for ( final String natGatewayId : timedOutNatGateways ) {
      try ( final TransactionResource tx = Entities.transactionFor( NatGateway.class ) ) {
        final NatGateway natGateway = natGateways.lookupByName( null, natGatewayId, Functions.<NatGateway>identity( ) );
        logger.info( "Deleting NAT gateway " + natGateway.getDisplayName( ) + " with state " + natGateway.getState( ) );
        natGateways.delete( natGateway );
        tx.commit( );
      } catch ( final VpcMetadataNotFoundException e ) {
        logger.info( "NAT gateway " + natGatewayId + " not found for deletion" );
      } catch ( final Exception e ) {
        logger.error( "Error deleting timed out NAT gateway " + natGatewayId, e );
      }
    }
  }

  /**
   * Progress deleting NAT gateway to deleted by cleaning up resources.
   */
  private void natGatewayDelete( ) {
    for ( final String natGatewayId : listNatGatewayIds( NatGateway.State.deleting ) ) {
      try ( final TransactionResource tx = Entities.transactionFor( NatGateway.class ) ) {
        final NatGateway natGateway = natGateways.lookupByName( null, natGatewayId, Functions.<NatGateway>identity( ) );
        releaseNatGatewayResources( natGateway );
        natGateway.setState( NatGateway.State.deleted );
        natGateway.markDeletion( );
        tx.commit( );
      } catch ( Exception e ) {
        if ( PersistenceExceptions.isStaleUpdate( e ) ) {
          logger.debug( "Conflict updating NAT gateway " + natGatewayId + " (will retry)" );
        } else {
          logger.error( "Error processing pending NAT gateway " + natGatewayId, e );
        }
      }
    }
  }

  private void releaseNatGatewayResources( final NatGateway natGateway ) throws VpcMetadataException {
    final Optional<NetworkInterface> networkInterface = NatGatewayHelper.cleanupResources( natGateway );
    if ( networkInterface.isPresent( ) ) {
      networkInterfaces.delete( networkInterface.get( ) );
    }
    natGateway.setVpc( null );
    natGateway.setSubnet( null );
    natGateway.setAssociationId( null );
    natGateway.setNetworkInterface( null );
  }

  /**
   * WARNING, route states here must be consistent with those calculated in NetworkInfoBroadcasts
   */
  private void routeStateCheck( ) {
    final List<RouteKey> keysToProcess = routesToCheck.stream( ).limit( 500 ).collect( Collectors.toList( ) );
    routesToCheck.removeAll( keysToProcess );
    for ( final RouteKey routeKey : keysToProcess ) {
      try ( final TransactionResource tx = Entities.transactionFor( RouteTable.class ) ) {
        final RouteTable routeTable =
            routeTables.lookupByName( null, routeKey.getRouteTableId( ), Functions.identity( ) );
        final java.util.Optional<Route> routeOptional = routeTable.getRoutes( ).stream( )
            .filter( route -> route.getDestinationCidr( ).equals( routeKey.getCidr( ) ) )
            .findFirst( );
        if ( routeOptional.isPresent( ) ) {
          final Route route = routeOptional.get( );
          Route.State newState = route.getState( );
          if ( route.getInternetGatewayId( ) != null ) {  // Internet gateway route
            try {
              final InternetGateway internetGateway =
                  internetGateways.lookupByName( null, route.getInternetGatewayId( ), Functions.identity( ) );
              newState = internetGateway.getVpc( ) != null ? Route.State.active : Route.State.blackhole;
            } catch ( final VpcMetadataNotFoundException e ) {
              newState = Route.State.blackhole;
            }
          } else if ( route.getNatGatewayId( ) != null ) { // NAT gateway route
            try {
              final NatGateway natGateway =
                  natGateways.lookupByName( null, route.getNatGatewayId( ), Functions.identity( ) );
              newState = natGateway.getState( ) == NatGateway.State.available ?
                  Route.State.active :
                  Route.State.blackhole;
            } catch ( final VpcMetadataNotFoundException e ) {
              newState = Route.State.blackhole;
            }
          } else if ( route.getNetworkInterfaceId( ) != null ) { // NAT instance route
            try {
              final NetworkInterface eni =
                  networkInterfaces.lookupByName( null, route.getNetworkInterfaceId( ), Functions.identity( ) );
              if ( !eni.isAttached( ) ) {
                if ( route.getInstanceId( ) != null || route.getInstanceAccountNumber( ) != null ) {
                  route.setInstanceId( null );
                  route.setInstanceAccountNumber( null );
                  routeTable.updateTimeStamps( );
                }
                newState = Route.State.blackhole;
              } else {
                if ( !eni.getInstance( ).getInstanceId( ).equals( route.getInstanceId( ) ) ) {
                  route.setInstanceId( eni.getInstance( ).getInstanceId( ) );
                  route.setInstanceAccountNumber( eni.getInstance( ).getOwnerAccountNumber( ) );
                  routeTable.updateTimeStamps( );
                }
                newState = VmInstance.VmState.RUNNING.apply( eni.getInstance( ) ) ?
                    Route.State.active :
                    Route.State.blackhole;
              }
            } catch ( final VpcMetadataNotFoundException e ) {
              if ( route.getInstanceId( ) != null ) {
                route.setInstanceId( null );
                route.setInstanceAccountNumber( null );
                routeTable.updateTimeStamps( );
              }
              newState = Route.State.blackhole;
            }
          } // else local route, always active
          if ( route.getState( ) != newState ) {
            route.setState( newState );
            routeTable.updateTimeStamps( );
          }
        }
        tx.commit( );
      } catch ( final VpcMetadataNotFoundException e ) {
        logger.debug( "Route table not found checking route state for " + routeKey );
      } catch ( Exception e ) {
        if ( PersistenceExceptions.isStaleUpdate( e ) ) {
          logger.debug( "Conflict checking route state for " + routeKey + " (will retry)" );
        } else {
          logger.error( "Error checking route state for " + routeKey, e );
        }
      }
    }
  }

  private static abstract class WorkflowTask {
    private volatile int count = 0;
    private final int factor;
    private final String task;

    protected WorkflowTask( final int factor, final String task ) {
      this.factor = factor;
      this.task = task;
    }

    protected final int calcFactor() {
      return factor / (int) Math.max( 1, SystemClock.RATE / 1000 );
    }

    protected final void perhapsWork() throws Exception {
      if ( ++count % calcFactor() == 0 ) {
        logger.trace( "Running VPC workflow task: " + task );
        doWork();
        logger.trace( "Completed VPC workflow task: " + task );
      }
    }

    abstract void doWork( ) throws Exception;
  }

  public static class VpcWorkflowEventListener implements EventListener<ClockTick> {
    private final VpcWorkflow vpcWorkflow = new VpcWorkflow(
        new EventFiringVpcInvalidator( ),
        new PersistenceInternetGateways( ),
        new PersistenceNatGateways( ),
        new PersistenceNetworkInterfaces( ),
        new PersistenceRouteTables( )
    );

    public static void register( ) {
      Listeners.register( ClockTick.class, new VpcWorkflowEventListener() );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      if ( Bootstrap.isOperational( ) &&
          Topology.isEnabledLocally( Eucalyptus.class ) &&
          Topology.isEnabled( Compute.class ) ) {
        vpcWorkflow.doWorkflow( );
      }
    }
  }

  public static class VpcRouteStateInvalidationEventListener implements EventListener<VpcRouteStateInvalidationEvent> {

    public static void register( ) {
      Listeners.register( VpcRouteStateInvalidationEvent.class, new VpcRouteStateInvalidationEventListener( ) );
    }

    @Override
    public void fireEvent( final VpcRouteStateInvalidationEvent event ) {
      if ( Bootstrap.isOperational( ) &&
          Topology.isEnabledLocally( Eucalyptus.class ) &&
          Topology.isEnabled( Compute.class ) ) {
        routesToCheck.addAll( event.getMessage( ) );
      }
    }
  }
}
