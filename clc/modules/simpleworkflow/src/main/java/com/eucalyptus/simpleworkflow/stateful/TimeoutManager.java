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
package com.eucalyptus.simpleworkflow.stateful;

import static com.eucalyptus.simpleworkflow.NotifyClient.NotifyTaskList;
import static com.eucalyptus.simpleworkflow.WorkflowExecution.DecisionStatus.Pending;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.simpleworkflow.ActivityTask;
import com.eucalyptus.simpleworkflow.ActivityTasks;
import com.eucalyptus.simpleworkflow.NotifyClient;
import com.eucalyptus.simpleworkflow.SwfMetadataException;
import com.eucalyptus.simpleworkflow.Timer;
import com.eucalyptus.simpleworkflow.Timers;
import com.eucalyptus.simpleworkflow.WorkflowExecution;
import com.eucalyptus.simpleworkflow.WorkflowExecutions;
import com.eucalyptus.simpleworkflow.WorkflowHistoryEvent;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.eucalyptus.simpleworkflow.common.model.ActivityTaskTimedOutEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.DecisionTaskScheduledEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.DecisionTaskTimedOutEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.TaskList;
import com.eucalyptus.simpleworkflow.common.model.TimerFiredEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionTimedOutEventAttributes;
import com.eucalyptus.simpleworkflow.persist.PersistenceActivityTasks;
import com.eucalyptus.simpleworkflow.persist.PersistenceTimers;
import com.eucalyptus.simpleworkflow.persist.PersistenceWorkflowExecutions;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Pair;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 *
 */
public class TimeoutManager {

  private static final Logger logger = Logger.getLogger( TimeoutManager.class );

  private final WorkflowExecutions workflowExecutions = new PersistenceWorkflowExecutions( );
  private final ActivityTasks activityTasks = new PersistenceActivityTasks( );
  private final Timers timers = new PersistenceTimers( );

  public void doTimeouts( ) {
    timeoutActivityTasks( );
    timeoutDecisionTasksAndWorkflows( );
  }

  public void doTimers( ) {
    try {
      final Set<NotifyTaskList> taskLists = Sets.newHashSet( );
      for ( final Timer timer : timers.listFired( Functions.<Timer>identity( ) ) ) {
        timers.updateByExample( timer, timer.getOwner( ), timer.getDisplayName( ), new Function<Timer, Void>( ) {
          @Override
          public Void apply( final Timer timer ) {
            final WorkflowExecution workflowExecution = timer.getWorkflowExecution( );
            workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                workflowExecution,
                new TimerFiredEventAttributes( )
                    .withStartedEventId( timer.getStartedEventId( ) )
                    .withTimerId( timer.getDisplayName( ) )
            ) );
            if ( workflowExecution.getDecisionStatus() != Pending ) {
              workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                  workflowExecution,
                  new DecisionTaskScheduledEventAttributes( )
                      .withTaskList( new TaskList( ).withName( workflowExecution.getTaskList( ) ) )
                      .withStartToCloseTimeout( String.valueOf( workflowExecution.getTaskStartToCloseTimeout( ) ) )
              ) );
              workflowExecution.setDecisionStatus( Pending );
              workflowExecution.setDecisionTimestamp( new Date( ) );
              addToNotifyLists( taskLists, workflowExecution );
            }
            Entities.delete( timer );
            return null;
          }
        } );
      }
      notifyLists( taskLists );
    } catch ( SwfMetadataException e ) {
      logger.error( "Error processing fired timers", e );
    }
  }

  public void doExpunge( ) {
    try {
      for ( final WorkflowExecution workflowExecution : workflowExecutions.listRetentionExpired( Functions.<WorkflowExecution>identity() ) ) {
        logger.debug( "Removing workflow execution with expired retention period: " +
            workflowExecution.getDisplayName() + "/" + workflowExecution.getWorkflowId() );
        workflowExecutions.deleteByExample( workflowExecution );
      }
    } catch ( final SwfMetadataException e ) {
      logger.error( "Error processing workflow execution retention expiry", e );
    }
  }

  private void timeoutActivityTasks( ) {
    try {
      final Set<NotifyTaskList> taskLists = Sets.newHashSet( );
      for ( final ActivityTask task : activityTasks.listTimedOut( Functions.<ActivityTask>identity( ) ) ) {
        activityTasks.updateByExample( task, task.getOwner( ), task.getDisplayName(), new Function<ActivityTask, Void>() {
          @Override
          public Void apply( final ActivityTask activityTask ) {
            final Pair<String,Date> timeout = activityTask.calculateNextTimeout( );
            if ( timeout != null ) {
              final WorkflowExecution workflowExecution = activityTask.getWorkflowExecution();
              workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                  workflowExecution,
                  new ActivityTaskTimedOutEventAttributes()
                      .withDetails( activityTask.getHeartbeatDetails() )
                      .withScheduledEventId( activityTask.getScheduledEventId() )
                      .withStartedEventId( activityTask.getStartedEventId() )
                      .withTimeoutType( timeout.getLeft() )
              ) );
              if ( workflowExecution.getDecisionStatus( ) != Pending ) {
                workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                    workflowExecution,
                    new DecisionTaskScheduledEventAttributes( )
                        .withTaskList( new TaskList( ).withName( workflowExecution.getTaskList( ) ) )
                        .withStartToCloseTimeout( String.valueOf( workflowExecution.getTaskStartToCloseTimeout( ) ) )
                ) );
                workflowExecution.setDecisionStatus( Pending );
                workflowExecution.setDecisionTimestamp( new Date( ) );
                addToNotifyLists( taskLists, workflowExecution );
              }
              Entities.delete( activityTask );
            }
            return null;
          }
        } );
      }
      notifyLists( taskLists );
    } catch ( SwfMetadataException e ) {
      logger.error( "Error processing activity task timeouts", e );
    }
  }

  private void timeoutDecisionTasksAndWorkflows( ) {
    try {
      final Set<NotifyTaskList> taskLists = Sets.newHashSet( );
      for ( final WorkflowExecution workflowExecution : workflowExecutions.listTimedOut( Functions.<WorkflowExecution>identity( ) ) ) {
        workflowExecutions.updateByExample( workflowExecution, workflowExecution.getOwner( ), workflowExecution.getDisplayName(), new Function<WorkflowExecution, Void>() {
          @Override
          public Void apply( final WorkflowExecution workflowExecution ) {
            final Date timeout = workflowExecution.calculateNextTimeout( );
            if ( timeout != null ) {
              if ( workflowExecution.isWorkflowTimedOut( ) ) {
                workflowExecution.closeWorkflow(
                    WorkflowExecution.CloseStatus.Timed_Out,
                    WorkflowHistoryEvent.create(
                        workflowExecution,
                        new WorkflowExecutionTimedOutEventAttributes()
                            .withTimeoutType( "START_TO_CLOSE" )
                            .withChildPolicy( workflowExecution.getChildPolicy() )
                ) );
              } else { // decision task timed out
                final List<WorkflowHistoryEvent> events = workflowExecution.getWorkflowHistory();
                final List<WorkflowHistoryEvent> reverseEvents = Lists.reverse( events );
                final WorkflowHistoryEvent scheduled = Iterables.find(
                    reverseEvents,
                    CollectionUtils.propertyPredicate( "DecisionTaskScheduled", WorkflowExecutions.WorkflowHistoryEventStringFunctions.EVENT_TYPE ) );
                final Optional<WorkflowHistoryEvent> previousStarted = Iterables.tryFind(
                    reverseEvents,
                    CollectionUtils.propertyPredicate( "DecisionTaskStarted", WorkflowExecutions.WorkflowHistoryEventStringFunctions.EVENT_TYPE ) );
                workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                    workflowExecution,
                    new DecisionTaskTimedOutEventAttributes( )
                        .withTimeoutType( "START_TO_CLOSE" )
                        .withScheduledEventId( scheduled.getEventId( ) )
                        .withStartedEventId( previousStarted.transform( WorkflowExecutions.WorkflowHistoryEventLongFunctions.EVENT_ID ).orNull( ) )
                ) );
                //TODO:STEVE: limit event history here ...
                workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                    workflowExecution,
                    new DecisionTaskScheduledEventAttributes( )
                        .withTaskList( new TaskList( ).withName( workflowExecution.getTaskList( ) ) )
                        .withStartToCloseTimeout( String.valueOf( workflowExecution.getTaskStartToCloseTimeout( ) ) )
                ) );
                workflowExecution.setDecisionStatus( Pending );
                workflowExecution.setDecisionTimestamp( new Date( ) );
                addToNotifyLists( taskLists, workflowExecution );
              }
            }
            return null;
          }
        } );
      }
      notifyLists( taskLists );
    } catch ( final SwfMetadataException e ) {
      logger.error( "Error processing workflow execution/decision task timeouts", e );
    }
  }

  private void addToNotifyLists( final Collection<NotifyTaskList> taskLists,
                                 final WorkflowExecution workflowExecution ) {
    taskLists.add( new NotifyTaskList(
        workflowExecution.getOwnerAccountNumber( ),
        workflowExecution.getDomainName( ),
        "decision",
        workflowExecution.getTaskList( ) ) );
  }

  private void notifyLists( final Set<NotifyTaskList> taskLists ) {
    for ( final NotifyTaskList list : taskLists ) {
      NotifyClient.notifyTaskList( list );
    }
  }

  public static class TimeoutManagerEventListener implements EventListener<ClockTick> {
    private final TimeoutManager timeoutManager = new TimeoutManager();

    public static void register( ) {
      Listeners.register( ClockTick.class, new TimeoutManagerEventListener( ) );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      if ( Bootstrap.isOperational( ) &&
          Topology.isEnabledLocally( PolledNotifications.class ) &&
          Topology.isEnabled( SimpleWorkflow.class ) ) {
        timeoutManager.doTimeouts( );
        timeoutManager.doTimers( );
        timeoutManager.doExpunge( );
      }
    }
  }

}
