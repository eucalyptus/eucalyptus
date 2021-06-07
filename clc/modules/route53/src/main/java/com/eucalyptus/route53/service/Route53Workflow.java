/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.event.SystemClock;
import com.eucalyptus.route53.common.Route53;
import com.eucalyptus.route53.service.persist.ChangeInfos;
import com.eucalyptus.route53.service.persist.Route53MetadataNotFoundException;
import com.eucalyptus.route53.service.persist.entities.ChangeInfo;
import com.eucalyptus.route53.service.persist.entities.ChangeInfo.Status;
import com.eucalyptus.route53.service.persist.entities.PersistenceChangeInfos;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;

/**
 *
 */
@SuppressWarnings("Guava")
public class Route53Workflow {

  private static final Logger logger = Logger.getLogger( Route53Workflow.class );

  private final List<WorkflowTask> workflowTasks = ImmutableList.<WorkflowTask>builder()
      .add( new WorkflowTask(  10, "Change.Completion" ) { @Override void doWork( ) throws Exception { changeCompletion( ); } } )
      .add( new WorkflowTask( 300, "Change.Expiry"     ) { @Override void doWork( ) throws Exception { changeExpiry( ); } } )
      .build( );

  private final ChangeInfos changeInfos;

  public Route53Workflow(
      final ChangeInfos changeInfos
  ) {
    this.changeInfos = changeInfos;
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

  /**
   * Update any changes that have been pending for the requisite interval
   */
  private void changeCompletion( ) {
    List<String> pendingChanges = Collections.emptyList( );
    try ( final TransactionResource tx = Entities.transactionFor( ChangeInfo.class ) ) {
      pendingChanges = changeInfos.list(
          null,
          Restrictions.and(
              Example.create( ChangeInfo.exampleWithState( Status.PENDING ) ),
              Restrictions.lt( "lastUpdateTimestamp", new Date( System.currentTimeMillis( ) - ChangeInfos.PENDING_AGE ) )
          ),
          Collections.emptyMap( ),
          Predicates.alwaysTrue( ),
          CloudMetadatas.toDisplayName( )
      );
    } catch ( final Exception e ) {
      logger.error( "Error listing pending changes", e );
    }

    for ( final String changeId : pendingChanges ) {
      try ( final TransactionResource tx = Entities.transactionFor( ChangeInfo.class ) ) {
        changeInfos.updateByExample(
            ChangeInfo.exampleWithName(null, changeId),
            null,
            changeId,
            changeInfo -> {
              logger.info( "Updating pending change " + changeInfo.getDisplayName( ) + " with state " + changeInfo.getState( ) );
              changeInfo.setState( Status.INSYNC );
              return changeId;
            } );
        tx.commit( );
      } catch ( final Route53MetadataNotFoundException e ) {
        logger.info( "Route53 change " + changeId + " not found for state transition" );
      } catch ( final Exception e ) {
        logger.error( "Error updating pending change " + changeId, e );
      }
    }
  }

  /**
   * Delete any changes that have expired
   */
  private void changeExpiry( ) {
    List<String> expiredChanges = Collections.emptyList( );
    try ( final TransactionResource tx = Entities.transactionFor( ChangeInfo.class ) ) {
      expiredChanges = changeInfos.list(
          null,
          Restrictions.lt( "lastUpdateTimestamp", new Date( System.currentTimeMillis( ) - ChangeInfos.EXPIRY_AGE ) ),
          Collections.emptyMap( ),
          Predicates.alwaysTrue( ),
          CloudMetadatas.toDisplayName( )
      );
    } catch ( final Exception e ) {
      logger.error( "Error listing expired changes", e );
    }

    for ( final String changeId : expiredChanges ) {
      try ( final TransactionResource tx = Entities.transactionFor( ChangeInfo.class ) ) {
        final ChangeInfo changeInfo = changeInfos.lookupByName( null, changeId, Predicates.alwaysTrue(), Functions.identity( ) );
        logger.info( "Deleting expired change " + changeInfo.getDisplayName( ) + " with state " + changeInfo.getState( ) );
        changeInfos.delete( changeInfo );
        tx.commit( );
      } catch ( final Route53MetadataNotFoundException e ) {
        logger.info( "Route53 change " + changeId + " not found for deletion" );
      } catch ( final Exception e ) {
        logger.error( "Error deleting expired change " + changeId, e );
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
        logger.trace( "Running route53 workflow task: " + task );
        doWork();
        logger.trace( "Completed route53 workflow task: " + task );
      }
    }

    abstract void doWork( ) throws Exception;
  }

  public static class Route53WorkflowEventListener implements EventListener<ClockTick> {
    private final Route53Workflow route53Workflow = new Route53Workflow(
        new PersistenceChangeInfos( )
    );

    public static void register( ) {
      Listeners.register( ClockTick.class, new Route53WorkflowEventListener() );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      if ( Bootstrap.isOperational( ) &&
          Topology.isEnabledLocally( Eucalyptus.class ) &&
          Topology.isEnabled( Route53.class ) ) {
        route53Workflow.doWorkflow( );
      }
    }
  }
}
