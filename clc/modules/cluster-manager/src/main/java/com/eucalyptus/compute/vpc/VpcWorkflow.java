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
package com.eucalyptus.compute.vpc;

import java.util.Collections;
import java.util.Date;
import java.util.List;
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
import com.eucalyptus.compute.common.internal.vpc.InternetGateways;
import com.eucalyptus.compute.common.internal.vpc.NatGateway;
import com.eucalyptus.compute.common.internal.vpc.NatGateways;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaces;
import com.eucalyptus.compute.common.internal.vpc.Subnet;
import com.eucalyptus.compute.common.internal.vpc.Vpc;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataException;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.compute.vpc.persist.PersistenceInternetGateways;
import com.eucalyptus.compute.vpc.persist.PersistenceNatGateways;
import com.eucalyptus.compute.vpc.persist.PersistenceNetworkInterfaces;
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

/**
 *
 */
public class VpcWorkflow {

  private static final Logger logger = Logger.getLogger( VpcWorkflow.class );

  private final VpcInvalidator vpcInvalidator;
  private final InternetGateways internetGateways;
  private final NatGateways natGateways;
  private final NetworkInterfaces networkInterfaces;

  private final List<WorkflowTask> workflowTasks = ImmutableList.<WorkflowTask>builder()
      .add( new WorkflowTask(  10, "NatGateway.SetupNetworkInterface" ) { @Override void doWork( ) throws Exception { natGatewaySetupNetworkInterface( ); } } )
      .add( new WorkflowTask(  10, "NatGateway.SetupElasticIP"        ) { @Override void doWork( ) throws Exception { natGatewaySetupElasticIp( ); } } )
      .add( new WorkflowTask(  10, "NatGateway.Delete"                ) { @Override void doWork( ) throws Exception { natGatewayDelete( ); } } )
      .add( new WorkflowTask(  30, "NatGateway.FailureCleanup"        ) { @Override void doWork( ) throws Exception { natGatewayFailureCleanup( ); } } )
      .add( new WorkflowTask( 300, "NatGateway.Timeout"               ) { @Override void doWork( ) throws Exception { natGatewayTimeout( ); } } )
      .build( );

  public VpcWorkflow(
      final VpcInvalidator vpcInvalidator,
      final InternetGateways internetGateways,
      final NatGateways natGateways,
      final NetworkInterfaces networkInterfaces
  ) {
    this.vpcInvalidator = vpcInvalidator;
    this.internetGateways = internetGateways;
    this.natGateways = natGateways;
    this.networkInterfaces = networkInterfaces;
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
    }
  }

  /**
   * Move pending NAT gateway to available by setting up the Elastic IP
   */
  private void natGatewaySetupElasticIp( ) {
    for ( final String natGatewayId : listNatGatewayIds( NatGateway.State.pending ) ) {
      boolean invalidate = false;
      try ( final TransactionResource tx = Entities.transactionFor( NatGateway.class ) ) {
        final NatGateway natGateway = natGateways.lookupByName( null, natGatewayId, Functions.<NatGateway>identity( ) );
        if ( natGateway.getNetworkInterface( ) != null &&
            natGateway.getPublicIpAddress( ) == null ) try { // do work for NAT gateway
          logger.info( "Setting up Elastic IP for pending NAT gateway " + natGatewayId );
          NatGatewayHelper.associatePublicAddress( natGateway );
          natGateway.setState( NatGateway.State.available );
          invalidate = true;
        } catch ( final ComputeException e ) { // NAT gateway creation failure
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

    for ( final String natGatewayId : failedNatGatewayIds ) {
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
        new PersistenceNetworkInterfaces( )
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
}
