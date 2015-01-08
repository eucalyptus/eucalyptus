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
package com.eucalyptus.simpleworkflow;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata;
import com.eucalyptus.simpleworkflow.common.model.*;
import com.eucalyptus.simpleworkflow.common.model.WorkflowType;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

/**
 *
 */
public interface WorkflowExecutions {

  Set<String> ACTIVITY_CLOSE_EVENT_TYPES = ImmutableSet.of( "ActivityTaskCompleted", "ActivityTaskFailed", "ActivityTaskTimedOut", "ActivityTaskCanceled" );

  Set<String> TIMER_CLOSE_EVENT_TYPES = ImmutableSet.of( "TimerCanceled", "TimerFired" );

  <T> List<T> listByExample( WorkflowExecution example,
                             Predicate<? super WorkflowExecution> filter,
                             Function<? super WorkflowExecution,T> transform ) throws SwfMetadataException;

  <T> List<T> listByExample( WorkflowExecution example,
                             Predicate<? super WorkflowExecution> filter,
                             Criterion criterion,
                             Map<String,String> aliases,
                             Function<? super WorkflowExecution,T> transform ) throws SwfMetadataException;

  <T> List<T> listTimedOut( long timestamp,
                            Function<? super WorkflowExecution,T> transform ) throws SwfMetadataException;

  <T> List<T> listRetentionExpired( long timestamp,
                                    Function<? super WorkflowExecution,T> transform ) throws SwfMetadataException;

  <T> T lookupByExample( WorkflowExecution example,
                         @Nullable OwnerFullName ownerFullName,
                         String key,
                         Predicate<? super WorkflowExecution> filter,
                         Function<? super WorkflowExecution,T> transform ) throws SwfMetadataException;

  <T> T updateByExample( WorkflowExecution example,
                         OwnerFullName ownerFullName,
                         String id,
                         Function<? super WorkflowExecution,T> updateTransform ) throws SwfMetadataException;

  WorkflowExecution save( WorkflowExecution workflowExecution ) throws SwfMetadataException;

  List<WorkflowExecution> deleteByExample( WorkflowExecution example ) throws SwfMetadataException;

  long countOpenByDomain( OwnerFullName ownerFullName, String domain ) throws SwfMetadataException;

  AbstractPersistentSupport<SimpleWorkflowMetadata.WorkflowExecutionMetadata,WorkflowExecution,SwfMetadataException> withRetries( );

  public static class Utils {
    private static final Logger logger = Logger.getLogger( WorkflowExecutions.class );

    public static void terminateWorkflowExecution( final WorkflowExecutions workflowExecutions,
                                                   final String cause,
                                                   final String accountNumber,
                                                   final String domain,
                                                   final String workflowId ) {
      try {
        final AccountFullName accountFullName = AccountFullName.getInstance( accountNumber );
        workflowExecutions.updateByExample(
            WorkflowExecution.exampleForOpenWorkflow( accountFullName, domain, workflowId ),
            accountFullName,
            workflowId,
            new Function<WorkflowExecution,Void>( ){
              @Override
              public Void apply( final WorkflowExecution workflowExecution ) {
                workflowExecution.closeWorkflow(
                    WorkflowExecution.CloseStatus.Terminated,
                    WorkflowHistoryEvent.create(
                        workflowExecution,
                        new WorkflowExecutionTerminatedEventAttributes( )
                            .withCause( cause )
                            .withChildPolicy( workflowExecution.getChildPolicy( ) ) )
                );
                return null;
              }
            }
        );
      } catch ( SwfMetadataException e ) {
        logger.error( "Error terminating workflow execution for account " + accountNumber + ", domain " + domain + ", " + workflowId, e );
      }
    }
  }

  @TypeMapper
  public enum WorkflowExecutionToWorkflowExecutionDetailTransform implements Function<WorkflowExecution,WorkflowExecutionDetail> {
    INSTANCE;

    @Nullable
    @Override
    public WorkflowExecutionDetail apply( @Nullable final WorkflowExecution execution ) {
      if ( execution == null ) return null;
      return new WorkflowExecutionDetail( )
          .withExecutionConfiguration( new WorkflowExecutionConfiguration( )
              .withChildPolicy( execution.getChildPolicy( ) )
              .withExecutionStartToCloseTimeout( Objects.toString( execution.getExecutionStartToCloseTimeout(), "NONE" ) )
              .withTaskList( new TaskList( )
                  .withName( execution.getTaskList( ) ) )
              .withTaskStartToCloseTimeout( Objects.toString( execution.getTaskStartToCloseTimeout( ), "NONE" ) ) )
          .withExecutionInfo( TypeMappers.transform( execution,WorkflowExecutionInfo.class ) )
          .withLatestActivityTaskTimestamp( execution.getLatestActivityTaskScheduled() )
          .withLatestExecutionContext( execution.getLatestExecutionContext() );
    }
  }

  @TypeMapper
  public enum WorkflowExecutionToWorkflowExecutionInfoTransform implements Function<WorkflowExecution,WorkflowExecutionInfo> {
    INSTANCE;

    @Nullable
    @Override
    public WorkflowExecutionInfo apply( @Nullable final WorkflowExecution execution ) {
      if ( execution == null ) return null;
      return new WorkflowExecutionInfo( )
          .withCancelRequested( execution.getCancelRequested( ) )
          .withCloseStatus( Objects.toString( execution.getCloseStatus( ), null ) )
          .withCloseTimestamp( execution.getCloseTimestamp( ) )
          .withExecution( new com.eucalyptus.simpleworkflow.common.model.WorkflowExecution( )
              .withRunId( execution.getDisplayName( ) )
              .withWorkflowId( execution.getWorkflowId( ) ) )
          .withExecutionStatus( Objects.toString( execution.getState( ) ) )
          .withStartTimestamp( execution.getCreationTimestamp( ) )
          .withTagList( execution.getTagList( ) )
          .withWorkflowType( new WorkflowType( )
              .withName( execution.getWorkflowType( ).getDisplayName( ) )
              .withVersion( execution.getWorkflowType( ).getWorkflowVersion( ) ) );
    }
  }

  @TypeMapper
  public enum WorkflowHistoryEventToHistoryEventTransform implements Function<WorkflowHistoryEvent,HistoryEvent> {
    INSTANCE;

    @Nullable
    @Override
    public HistoryEvent apply( @Nullable final WorkflowHistoryEvent event ) {
      if ( event == null ) return null;
      final HistoryEvent historyEvent = new HistoryEvent( )
          .withEventId( event.getEventId() )
          .withEventType( event.getEventType() )
          .withEventTimestamp( event.getCreationTimestamp() );
      final WorkflowEventAttributes attributes = event.toAttributes( );
      attributes.attach( historyEvent );
      return historyEvent;
    }
  }

  public enum WorkflowExecutionStringFunctions implements Function<WorkflowExecution,String> {
    DOMAIN_UUID {
      @Nullable
      @Override
      public String apply( @Nullable final WorkflowExecution workflowExecution ) {
        return workflowExecution == null ?
            null :
            workflowExecution.getDomainUuid( );
      }
    }
  }

  public enum WorkflowExecutionInfoDateFunctions implements Function<WorkflowExecutionInfo,Date> {
    START_TIMESTAMP {
      @Nullable
      @Override
      public Date apply( @Nullable final WorkflowExecutionInfo workflowExecutionInfo ) {
        return workflowExecutionInfo == null ?
            null :
            workflowExecutionInfo.getStartTimestamp( );
      }
    }
  }

  public enum WorkflowHistoryEventStringFunctions implements Function<WorkflowHistoryEvent,String> {
    EVENT_TYPE {
      @Nullable
      @Override
      public String apply( @Nullable final WorkflowHistoryEvent workflowHistoryEvent ) {
        return workflowHistoryEvent == null ?
            null :
            workflowHistoryEvent.getEventType( );
      }
    }
  }

  public enum WorkflowHistoryEventLongFunctions implements Function<WorkflowHistoryEvent,Long> {
    EVENT_ID {
      @Nullable
      @Override
      public Long apply( @Nullable final WorkflowHistoryEvent workflowHistoryEvent ) {
        return workflowHistoryEvent == null ?
            null :
            workflowHistoryEvent.getEventId();
      }
    }
  }
}
