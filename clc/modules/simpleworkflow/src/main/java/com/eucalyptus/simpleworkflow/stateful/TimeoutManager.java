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
package com.eucalyptus.simpleworkflow.stateful;

import static com.eucalyptus.simpleworkflow.NotifyClient.NotifyTaskList;
import static com.eucalyptus.simpleworkflow.SimpleWorkflowProperties.getWorkflowExecutionDurationMillis;
import static com.eucalyptus.simpleworkflow.WorkflowExecution.DecisionStatus.Idle;
import static com.eucalyptus.simpleworkflow.WorkflowExecution.DecisionStatus.Pending;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.simpleworkflow.ActivityTask;
import com.eucalyptus.simpleworkflow.ActivityTasks;
import com.eucalyptus.simpleworkflow.ActivityType;
import com.eucalyptus.simpleworkflow.ActivityTypes;
import com.eucalyptus.simpleworkflow.Domain;
import com.eucalyptus.simpleworkflow.Domains;
import com.eucalyptus.simpleworkflow.NotifyClient;
import com.eucalyptus.simpleworkflow.SwfMetadataException;
import com.eucalyptus.simpleworkflow.SwfMetadataNotFoundException;
import com.eucalyptus.simpleworkflow.Timer;
import com.eucalyptus.simpleworkflow.Timers;
import com.eucalyptus.simpleworkflow.WorkflowExecution;
import com.eucalyptus.simpleworkflow.WorkflowExecutions;
import com.eucalyptus.simpleworkflow.WorkflowHistoryEvent;
import com.eucalyptus.simpleworkflow.WorkflowLock;
import com.eucalyptus.simpleworkflow.WorkflowType;
import com.eucalyptus.simpleworkflow.WorkflowTypes;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.eucalyptus.simpleworkflow.common.model.ActivityTaskTimedOutEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.DecisionTaskScheduledEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.DecisionTaskTimedOutEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.TaskList;
import com.eucalyptus.simpleworkflow.common.model.TimerFiredEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionTimedOutEventAttributes;
import com.eucalyptus.simpleworkflow.persist.PersistenceActivityTasks;
import com.eucalyptus.simpleworkflow.persist.PersistenceActivityTypes;
import com.eucalyptus.simpleworkflow.persist.PersistenceDomains;
import com.eucalyptus.simpleworkflow.persist.PersistenceTimers;
import com.eucalyptus.simpleworkflow.persist.PersistenceWorkflowExecutions;
import com.eucalyptus.simpleworkflow.persist.PersistenceWorkflowTypes;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
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
  private final WorkflowTypes workflowTypes = new PersistenceWorkflowTypes( );
  private final ActivityTasks activityTasks = new PersistenceActivityTasks( );
  private final ActivityTypes activityTypes = new PersistenceActivityTypes( );
  private final Domains domains = new PersistenceDomains( );
  private final Timers timers = new PersistenceTimers( );

  public void doTimeouts( ) {
    timeoutActivityTasks( );
    timeoutDecisionTasksAndWorkflows( );
  }

  public void doTimers( ) {
    final Set<NotifyTaskList> taskLists = Sets.newHashSet( );
    try {
      for ( final Timer timer : timers.listFired( Functions.<Timer>identity( ) ) ) try {
        try ( final WorkflowLock lock = WorkflowLock.lock(
            timer.getOwnerAccountNumber( ),
            timer.getDomainUuid( ),
            timer.getWorkflowRunId( ) ) ) {
          workflowExecutions.withRetries( ).updateByExample(
              WorkflowExecution.exampleWithName( timer.getOwner( ), timer.getWorkflowRunId( ) ),
              timer.getOwner( ),
              timer.getWorkflowRunId( ),
              new Function<WorkflowExecution, Void>( ){
                @Nullable
                @Override
                public Void apply( final WorkflowExecution workflowExecution ) {
                  try {
                    timers.updateByExample(
                        timer,
                        timer.getOwner( ),
                        timer.getDisplayName( ),
                        new Function<Timer, Void>( ) {
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
                              if ( workflowExecution.getDecisionStatus() == Idle ) {
                                workflowExecution.setDecisionStatus( Pending );
                                workflowExecution.setDecisionTimestamp( new Date( ) );
                                addToNotifyLists( taskLists, workflowExecution );
                              }
                            }
                            Entities.delete( timer );
                            return null;
                          }
                        } );
                  } catch ( SwfMetadataException e ) {
                    throw Exceptions.toUndeclared( e );
                  }
                  return null;
                }
              }
          );
        }
      } catch ( SwfMetadataException e ) {
        if ( !handleException( e ) ) {
          logger.error( "Error processing fired timer: " +  timer.getWorkflowRunId() + "/" + timer.getStartedEventId( ), e );
        }
      }
    } catch ( SwfMetadataException e ) {
      logger.error( "Error processing fired timers", e );
    }
    notifyLists( taskLists );
  }

  public void doExpunge( ) {
    try {
      for ( final WorkflowExecution workflowExecution :
          workflowExecutions.listRetentionExpired( System.currentTimeMillis( ), Functions.<WorkflowExecution>identity( ) ) ) {
        logger.debug( "Removing workflow execution with expired retention period: " +
            workflowExecution.getDisplayName( ) + "/" + workflowExecution.getWorkflowId( ) );
        workflowExecutions.deleteByExample( workflowExecution );
      }
    } catch ( final SwfMetadataException e ) {
      logger.error( "Error processing workflow execution retention expiry", e );
    }

    try {
      for ( final ActivityType activityType :
          activityTypes.listDeprecatedExpired( System.currentTimeMillis( ), Functions.<ActivityType>identity( ) ) ) {
        logger.debug( "Removing expired deprecated activity type: " +
            activityType.getDisplayName( ) + "/" + activityType.getActivityVersion( ) );
        activityTypes.deleteByExample( activityType );
      }
    } catch ( final SwfMetadataException e ) {
      logger.error( "Error processing deprecated activity type expiry", e );
    }

    try {
      for ( final WorkflowType workflowType :
          workflowTypes.listDeprecatedExpired( System.currentTimeMillis( ), Functions.<WorkflowType>identity( ) ) ) {
        logger.debug( "Removing expired deprecated workflow type: " +
            workflowType.getDisplayName( ) + "/" + workflowType.getWorkflowVersion( ) );
        workflowTypes.deleteByExample( workflowType );
      }
    } catch ( final SwfMetadataException e ) {
      logger.error( "Error processing deprecated workflow type expiry", e );
    }

    try {
      for ( final Domain domain :
          domains.listDeprecatedExpired( System.currentTimeMillis( ), Functions.<Domain>identity( ) ) ) {
        logger.debug( "Removing domain with expired retention period: " + domain.getDisplayName( ) );
        domains.deleteByExample( domain );
      }
    } catch ( final SwfMetadataException e ) {
      logger.error( "Error processing domain retention expiry", e );
    }
  }

  private void timeoutActivityTasks( ) {
    final Set<NotifyTaskList> taskLists = Sets.newHashSet( );
    try {
      for ( final ActivityTask task : activityTasks.listTimedOut( Functions.<ActivityTask>identity( ) ) ) {
        try ( final WorkflowLock lock =
                  WorkflowLock.lock( task.getOwnerAccountNumber( ), task.getDomainUuid( ), task.getWorkflowRunId( ) ) ) {
          activityTasks.withRetries( ).updateByExample(
              task,
              task.getOwner( ),
              task.getDisplayName( ),
              new Function<ActivityTask, Void>() {
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
                  if ( workflowExecution.getDecisionStatus() == Idle ) {
                    workflowExecution.setDecisionStatus( Pending );
                    workflowExecution.setDecisionTimestamp( new Date( ) );
                    addToNotifyLists( taskLists, workflowExecution );
                  }
                }
                Entities.delete( activityTask );
              }
              return null;
            }
          } );
        } catch ( SwfMetadataException e ) {
          if ( !handleException( e ) ) {
            if ( Exceptions.isCausedBy( e, SwfMetadataNotFoundException.class ) ) {
              logger.debug( "Activity task not found for timeout: " + task.getWorkflowRunId( ) + "/" + task.getScheduledEventId( ) );
            } else {
              logger.error( "Error processing activity task timeout: " + task.getWorkflowRunId( ) + "/" + task.getScheduledEventId( ), e );
            }
          }
        }
      }
    } catch ( SwfMetadataException e ) {
      logger.error( "Error processing activity task timeouts", e );
    }
    notifyLists( taskLists );
  }

  private void timeoutDecisionTasksAndWorkflows( ) {
    final Set<NotifyTaskList> taskLists = Sets.newHashSet( );
    try {
      final long now = System.currentTimeMillis();
      for ( final WorkflowExecution workflowExecution :
          workflowExecutions.listTimedOut( now, Functions.<WorkflowExecution>identity( ) ) ) {
        try ( final WorkflowLock lock = WorkflowLock.lock(
            workflowExecution.getOwnerAccountNumber( ),
            workflowExecution.getDomainUuid( ),
            workflowExecution.getDisplayName( ) ) ) {
          workflowExecutions.withRetries( ).updateByExample(
              workflowExecution,
              workflowExecution.getOwner( ),
              workflowExecution.getDisplayName( ),
              new Function<WorkflowExecution, Void>() {
            @Override
            public Void apply( final WorkflowExecution workflowExecution ) {
              final Date timeout = workflowExecution.calculateNextTimeout( );
              if ( timeout != null ) {
                if ( workflowExecution.isWorkflowTimedOut( now, getWorkflowExecutionDurationMillis( ) ) ) {
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
        } catch ( final SwfMetadataException e ) {
          if ( !handleException( e ) ) {
            logger.error( "Error processing workflow execution/decision task timeout: " + workflowExecution.getDisplayName(), e );
          }
        }
      }
    } catch ( final SwfMetadataException e ) {
      logger.error( "Error processing workflow execution/decision task timeouts", e );
    }
    notifyLists( taskLists );
  }

  private boolean handleException( final Throwable e ) {
    final WorkflowExecution.WorkflowHistorySizeLimitException historySizeLimitCause =
        Exceptions.findCause( e, WorkflowExecution.WorkflowHistorySizeLimitException.class );
    if ( historySizeLimitCause != null ) {
      WorkflowExecutions.Utils.terminateWorkflowExecution(
          workflowExecutions,
          "EVENT_LIMIT_EXCEEDED",
          historySizeLimitCause.getAccountNumber( ),
          historySizeLimitCause.getDomain( ),
          historySizeLimitCause.getWorkflowId( ) );
      return true;
    }
    return false;
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
