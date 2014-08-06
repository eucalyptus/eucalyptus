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

import static com.eucalyptus.simpleworkflow.WorkflowExecution.DecisionStatus.*;
import java.lang.System;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.AuthQuotaException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadatas;
import com.eucalyptus.simpleworkflow.common.model.ActivityTaskCancelRequestedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.ActivityTaskCanceledEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.ActivityTaskCompletedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.ActivityTaskFailedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.ActivityTaskScheduledEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.ActivityTaskStartedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.ActivityTaskStatus;
import com.eucalyptus.simpleworkflow.common.model.ActivityTypeDetail;
import com.eucalyptus.simpleworkflow.common.model.ActivityTypeInfo;
import com.eucalyptus.simpleworkflow.common.model.ActivityTypeInfos;
import com.eucalyptus.simpleworkflow.common.model.CancelTimerDecisionAttributes;
import com.eucalyptus.simpleworkflow.common.model.CancelTimerFailedCause;
import com.eucalyptus.simpleworkflow.common.model.CancelTimerFailedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.CancelWorkflowExecutionDecisionAttributes;
import com.eucalyptus.simpleworkflow.common.model.CompleteWorkflowExecutionDecisionAttributes;
import com.eucalyptus.simpleworkflow.common.model.ContinueAsNewWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.ContinueAsNewWorkflowExecutionFailedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.CountClosedWorkflowExecutionsRequest;
import com.eucalyptus.simpleworkflow.common.model.CountOpenWorkflowExecutionsRequest;
import com.eucalyptus.simpleworkflow.common.model.CountPendingActivityTasksRequest;
import com.eucalyptus.simpleworkflow.common.model.CountPendingDecisionTasksRequest;
import com.eucalyptus.simpleworkflow.common.model.Decision;
import com.eucalyptus.simpleworkflow.common.model.DecisionTask;
import com.eucalyptus.simpleworkflow.common.model.DecisionTaskCompletedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.DecisionTaskScheduledEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.DecisionTaskStartedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.DeprecateActivityTypeRequest;
import com.eucalyptus.simpleworkflow.common.model.DeprecateDomainRequest;
import com.eucalyptus.simpleworkflow.common.model.DeprecateWorkflowTypeRequest;
import com.eucalyptus.simpleworkflow.common.model.DescribeActivityTypeRequest;
import com.eucalyptus.simpleworkflow.common.model.DescribeDomainRequest;
import com.eucalyptus.simpleworkflow.common.model.DescribeWorkflowExecutionRequest;
import com.eucalyptus.simpleworkflow.common.model.DescribeWorkflowTypeRequest;
import com.eucalyptus.simpleworkflow.common.model.DomainDetail;
import com.eucalyptus.simpleworkflow.common.model.DomainInfo;
import com.eucalyptus.simpleworkflow.common.model.DomainInfos;
import com.eucalyptus.simpleworkflow.common.model.FailWorkflowExecutionDecisionAttributes;
import com.eucalyptus.simpleworkflow.common.model.GetWorkflowExecutionHistoryRequest;
import com.eucalyptus.simpleworkflow.common.model.History;
import com.eucalyptus.simpleworkflow.common.model.HistoryEvent;
import com.eucalyptus.simpleworkflow.common.model.ListActivityTypesRequest;
import com.eucalyptus.simpleworkflow.common.model.ListClosedWorkflowExecutionsRequest;
import com.eucalyptus.simpleworkflow.common.model.ListDomainsRequest;
import com.eucalyptus.simpleworkflow.common.model.ListOpenWorkflowExecutionsRequest;
import com.eucalyptus.simpleworkflow.common.model.ListWorkflowTypesRequest;
import com.eucalyptus.simpleworkflow.common.model.MarkerRecordedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.PendingTaskCount;
import com.eucalyptus.simpleworkflow.common.model.PollForActivityTaskRequest;
import com.eucalyptus.simpleworkflow.common.model.PollForDecisionTaskRequest;
import com.eucalyptus.simpleworkflow.common.model.RecordActivityTaskHeartbeatRequest;
import com.eucalyptus.simpleworkflow.common.model.RecordMarkerDecisionAttributes;
import com.eucalyptus.simpleworkflow.common.model.RegisterActivityTypeRequest;
import com.eucalyptus.simpleworkflow.common.model.RegisterDomainRequest;
import com.eucalyptus.simpleworkflow.common.model.RegisterWorkflowTypeRequest;
import com.eucalyptus.simpleworkflow.common.model.RequestCancelActivityTaskDecisionAttributes;
import com.eucalyptus.simpleworkflow.common.model.RequestCancelActivityTaskFailedCause;
import com.eucalyptus.simpleworkflow.common.model.RequestCancelActivityTaskFailedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.RequestCancelExternalWorkflowExecutionDecisionAttributes;
import com.eucalyptus.simpleworkflow.common.model.RequestCancelExternalWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.RequestCancelExternalWorkflowExecutionFailedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.RequestCancelWorkflowExecutionRequest;
import com.eucalyptus.simpleworkflow.common.model.RespondActivityTaskCanceledRequest;
import com.eucalyptus.simpleworkflow.common.model.RespondActivityTaskCompletedRequest;
import com.eucalyptus.simpleworkflow.common.model.RespondActivityTaskFailedRequest;
import com.eucalyptus.simpleworkflow.common.model.RespondDecisionTaskCompletedRequest;
import com.eucalyptus.simpleworkflow.common.model.Run;
import com.eucalyptus.simpleworkflow.common.model.ScheduleActivityTaskDecisionAttributes;
import com.eucalyptus.simpleworkflow.common.model.SignalExternalWorkflowExecutionDecisionAttributes;
import com.eucalyptus.simpleworkflow.common.model.SignalExternalWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.SignalExternalWorkflowExecutionFailedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.SignalWorkflowExecutionRequest;
import com.eucalyptus.simpleworkflow.common.model.SimpleWorkflowMessage;
import com.eucalyptus.simpleworkflow.common.model.StartChildWorkflowExecutionDecisionAttributes;
import com.eucalyptus.simpleworkflow.common.model.StartChildWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.StartChildWorkflowExecutionFailedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.StartTimerDecisionAttributes;
import com.eucalyptus.simpleworkflow.common.model.StartTimerFailedCause;
import com.eucalyptus.simpleworkflow.common.model.StartTimerFailedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.StartWorkflowExecutionRequest;
import com.eucalyptus.simpleworkflow.common.model.TaskList;
import com.eucalyptus.simpleworkflow.common.model.TerminateWorkflowExecutionRequest;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionCancelRequestedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionCanceledEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionCompletedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionCount;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionDetail;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionFailedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionInfos;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionSignaledEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionStartedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionTerminatedEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.WorkflowTypeDetail;
import com.eucalyptus.simpleworkflow.common.model.WorkflowTypeInfo;
import com.eucalyptus.simpleworkflow.common.model.WorkflowTypeInfos;
import com.eucalyptus.simpleworkflow.tokens.TaskToken;
import com.eucalyptus.simpleworkflow.tokens.TaskTokenException;
import com.eucalyptus.simpleworkflow.tokens.TaskTokenManager;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedType;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.ws.Role;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 *
 */
@ComponentNamed
public class SimpleWorkflowService {

  private static final Logger logger = Logger.getLogger( SimpleWorkflowService.class );
  private static final Object pollNotifier = new Object( );

  private final Domains domains;
  private final ActivityTasks activityTasks;
  private final ActivityTypes activityTypes;
  private final WorkflowTypes workflowTypes;
  private final WorkflowExecutions workflowExecutions;
  private final TaskTokenManager taskTokenManager;

  @Inject
  public SimpleWorkflowService( final Domains domains,
                                final ActivityTasks activityTasks,
                                final ActivityTypes activityTypes,
                                final WorkflowTypes workflowTypes,
                                final WorkflowExecutions workflowExecutions,
                                final TaskTokenManager taskTokenManager ) {
    this.domains = domains;
    this.activityTasks = activityTasks;
    this.activityTypes = activityTypes;
    this.workflowTypes = workflowTypes;
    this.workflowExecutions = workflowExecutions;
    this.taskTokenManager = taskTokenManager;
  }

  public SimpleWorkflowMessage registerDomain( final RegisterDomainRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    allocate( new Supplier<Domain>( ) {
      @Override
      public Domain get( ) {
        try {
          final Domain domain = Domain.create(
              userFullName,
              request.getName( ),
              request.getDescription( ),
              parsePeriod( request.getWorkflowExecutionRetentionPeriodInDays( ) ) );
          return domains.save( domain );
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    }, Domain.class, request.getName( ) );
    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public SimpleWorkflowMessage deprecateDomain( final DeprecateDomainRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final Predicate<? super Domain> accessible =
        SimpleWorkflowMetadatas.filteringFor( Domain.class ).byPrivileges( ) .buildPredicate( );
    try {
      domains.updateByExample( Domain.exampleWithName( accountFullName, request.getName( ) ), accountFullName, request.getName( ), new Callback<Domain>( ) {
        @Override
        public void fire( final Domain domain ) {
          if ( accessible.apply( domain ) ) {
            if  ( domain.getState( ) == Domain.Status.Deprecated ) {
              throw Exceptions.toUndeclared( new SimpleWorkflowClientException(
                  "DomainDeprecatedFault",
                  "Domain already deprecated: " + request.getName( ) ) );
            }
            domain.setState( Domain.Status.Deprecated );
          }
        }
      } );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException( "UnknownResourceFault", "Domain not found: " + request.getName( ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public DomainInfos listDomains( final ListDomainsRequest request ) throws SimpleWorkflowException {
    final DomainInfos domainInfos = new DomainInfos( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final Predicate<? super Domain> requestedAndAccessible = SimpleWorkflowMetadatas.filteringFor( Domain.class )
        .byProperty( Optional.fromNullable( request.getRegistrationStatus( ) ).asSet( ), Domains.StringFunctions.REGISTRATION_STATUS )
        .byPrivileges( )
        .buildPredicate( );
    try {
      domainInfos.getDomainInfos( ).addAll( domains.list(
          accountFullName,
          Restrictions.conjunction( ),
          Collections.<String, String>emptyMap( ),
          requestedAndAccessible,
          TypeMappers.lookup( Domain.class, DomainInfo.class ) ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return request.reply( domainInfos );
  }

  public DomainDetail describeDomain( final DescribeDomainRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final Predicate<? super Domain> requestedAndAccessible = SimpleWorkflowMetadatas.filteringFor( Domain.class )
        .byId( Lists.newArrayList( request.getName( ) ) )
        .byPrivileges( )
        .buildPredicate( );
    try {
      return request.reply( domains.lookupByExample(
          Domain.exampleWithName( accountFullName, request.getName( ) ),
          accountFullName,
          request.getName( ),
          requestedAndAccessible,
          TypeMappers.lookup( Domain.class, DomainDetail.class ) ) );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException( "UnknownResourceFault", "Domain not found: " + request.getName( ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }
  }

  public SimpleWorkflowMessage registerActivityType( final RegisterActivityTypeRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    allocate( new Supplier<ActivityType>( ) {
      @Override
      public ActivityType get( ) {
        try {
          final Domain domain =
              domains.lookupByName( accountFullName, request.getDomain( ), Functions.<Domain>identity( ) );
          final ActivityType activityType = ActivityType.create(
              userFullName,
              request.getName( ),
              request.getVersion( ),
              domain,
              request.getDescription( ),
              request.getDefaultTaskList( ) == null ? null : request.getDefaultTaskList( ).getName( ),
              parsePeriod( request.getDefaultTaskHeartbeatTimeout( ) ),
              parsePeriod( request.getDefaultTaskScheduleToCloseTimeout( ) ),
              parsePeriod( request.getDefaultTaskScheduleToStartTimeout( ) ),
              parsePeriod( request.getDefaultTaskStartToCloseTimeout( ) )
          );
          return activityTypes.save( activityType );
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    }, ActivityType.class, request.getName( ) );
    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public SimpleWorkflowMessage deprecateActivityType( final DeprecateActivityTypeRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final Predicate<? super ActivityType> accessible =
        SimpleWorkflowMetadatas.filteringFor( ActivityType.class ).byPrivileges( ) .buildPredicate( );
    try {
      activityTypes.updateByExample(
          ActivityType.exampleWithUniqueName(
              accountFullName,
              request.getDomain( ),
              request.getActivityType( ).getName( ),
              request.getActivityType( ).getVersion( ) ),
          accountFullName,
          request.getActivityType( ).getName( ),
          new Callback<ActivityType>( ) {
            @Override
            public void fire( final ActivityType activityType ) {
              if ( accessible.apply( activityType ) ) {
                if ( activityType.getState( ) == ActivityType.Status.Deprecated ) {
                  throw Exceptions.toUndeclared( new SimpleWorkflowClientException(
                      "TypeDeprecatedFault",
                      "Activity type already deprecated: " + request.getActivityType( ).getName( ) ) );
                }
                activityType.setState( ActivityType.Status.Deprecated );
                activityType.setDeprecationTimestamp( new Date( ) );
              }
            }
          } );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          "Activity type not found: " + request.getActivityType( ).getName( ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public ActivityTypeInfos listActivityTypes( final ListActivityTypesRequest request ) throws SimpleWorkflowException {
    final ActivityTypeInfos activityTypeInfos = new ActivityTypeInfos( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final Predicate<? super ActivityType> requestedAndAccessible = SimpleWorkflowMetadatas.filteringFor( ActivityType.class )
        .byProperty( Optional.fromNullable( request.getDomain( ) ).asSet( ), ActivityTypes.StringFunctions.DOMAIN )
        .byProperty( Optional.fromNullable( request.getRegistrationStatus( ) ).asSet( ), ActivityTypes.StringFunctions.REGISTRATION_STATUS )
        .byId( Optional.fromNullable( request.getName( ) ).asSet( ) )
        .byPrivileges( )
        .buildPredicate( );
    try {
      activityTypeInfos.getTypeInfos( ).addAll( activityTypes.list(
          accountFullName,
          Restrictions.conjunction( ),
          Collections.<String, String>emptyMap( ),
          requestedAndAccessible,
          TypeMappers.lookup( ActivityType.class, ActivityTypeInfo.class ) ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return request.reply( activityTypeInfos );
  }

  public ActivityTypeDetail describeActivityType( final DescribeActivityTypeRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final Predicate<? super ActivityType> accessible =
        SimpleWorkflowMetadatas.filteringFor( ActivityType.class ).byPrivileges().buildPredicate();
    try {
      return request.reply( activityTypes.lookupByExample(
          ActivityType.exampleWithUniqueName(
              accountFullName,
              request.getDomain(),
              request.getActivityType().getName(),
              request.getActivityType().getVersion() ),
          accountFullName,
          request.getActivityType( ).getName(),
          accessible,
          TypeMappers.lookup( ActivityType.class, ActivityTypeDetail.class ) ) );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          "Activity type not found: " + request.getActivityType( ).getName() );
    } catch ( Exception e ) {
      throw handleException( e );
    }
  }

  public SimpleWorkflowMessage registerWorkflowType( final RegisterWorkflowTypeRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    allocate( new Supplier<WorkflowType>( ) {
      @Override
      public WorkflowType get( ) {
        try {
          final Domain domain =
              domains.lookupByName( accountFullName, request.getDomain( ), Functions.<Domain>identity( ) );
          final WorkflowType workflowType = WorkflowType.create(
              userFullName,
              request.getName( ),
              request.getVersion( ),
              domain,
              request.getDescription( ),
              request.getDefaultTaskList( ) == null ? null : request.getDefaultTaskList( ).getName( ),
              request.getDefaultChildPolicy( ),
              parsePeriod( request.getDefaultExecutionStartToCloseTimeout( ) ), //TODO:STEVE: validate not NONE
              parsePeriod( request.getDefaultTaskStartToCloseTimeout( ) )
          );
          return workflowTypes.save( workflowType );
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    }, WorkflowType.class, request.getName( ) );
    return request.reply( new SimpleWorkflowMessage( ) );

  }

  public SimpleWorkflowMessage deprecateWorkflowType( final DeprecateWorkflowTypeRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final Predicate<? super WorkflowType> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowType.class ).byPrivileges( ) .buildPredicate( );
    try {
      workflowTypes.updateByExample( WorkflowType.exampleWithUniqueName(
              accountFullName,
              request.getDomain( ),
              request.getWorkflowType( ).getName( ),
              request.getWorkflowType( ).getVersion( ) ),
          accountFullName,
          request.getWorkflowType( ).getName( ),
          new Callback<WorkflowType>( ) {
            @Override
            public void fire( final WorkflowType workflowType ) {
              if ( accessible.apply( workflowType ) ) {
                if ( workflowType.getState( ) == WorkflowType.Status.Deprecated ) {
                  throw Exceptions.toUndeclared( new SimpleWorkflowClientException(
                      "TypeDeprecatedFault",
                      "Workflow type already deprecated: " + request.getWorkflowType( ).getName( ) ) );
                }
                workflowType.setState( WorkflowType.Status.Deprecated );
                workflowType.setDeprecationTimestamp( new Date( ) );
              }
            }
          } );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          "Workflow type not found: " + request.getWorkflowType( ).getName( ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public WorkflowTypeInfos listWorkflowTypes( final ListWorkflowTypesRequest request ) throws SimpleWorkflowException {
    final WorkflowTypeInfos workflowTypeInfos = new WorkflowTypeInfos( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final Predicate<? super WorkflowType> requestedAndAccessible = SimpleWorkflowMetadatas.filteringFor( WorkflowType.class )
        .byProperty( Optional.fromNullable( request.getDomain( ) ).asSet( ), WorkflowTypes.StringFunctions.DOMAIN )
        .byProperty( Optional.fromNullable( request.getRegistrationStatus( ) ).asSet( ), WorkflowTypes.StringFunctions.REGISTRATION_STATUS )
        .byId( Optional.fromNullable( request.getName( ) ).asSet( ) )
        .byPrivileges( )
        .buildPredicate( );
    try {
      workflowTypeInfos.getTypeInfos( ).addAll( workflowTypes.list( //TODO:STEVE: result ordering
          accountFullName,
          Restrictions.conjunction( ),
          Collections.<String, String>emptyMap( ),
          requestedAndAccessible,
          TypeMappers.lookup( WorkflowType.class, WorkflowTypeInfo.class ) ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return request.reply( workflowTypeInfos );
  }

  public WorkflowTypeDetail describeWorkflowType( final DescribeWorkflowTypeRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final Predicate<? super WorkflowType> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowType.class ).byPrivileges( ).buildPredicate( );
    try {
      return request.reply( workflowTypes.lookupByExample(
          WorkflowType.exampleWithUniqueName(
              accountFullName,
              request.getDomain( ),
              request.getWorkflowType( ).getName( ),
              request.getWorkflowType( ).getVersion( ) ),
          accountFullName,
          request.getWorkflowType( ).getName( ),
          accessible,
          TypeMappers.lookup( WorkflowType.class, WorkflowTypeDetail.class ) ) );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          "Workflow type not found: " + request.getWorkflowType( ).getName( ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }
  }

  public WorkflowExecutionDetail describeWorkflowExecution( final DescribeWorkflowExecutionRequest request ) throws SimpleWorkflowException {
    return request.reply( new WorkflowExecutionDetail( ) ); //TODO:STEVE: counts and listings - EUCA-9623
  }

  public WorkflowExecutionCount countClosedWorkflowExecutions( final CountClosedWorkflowExecutionsRequest request ) throws SimpleWorkflowException {
    return request.reply( new WorkflowExecutionCount( ) );
  }

  public WorkflowExecutionCount countOpenWorkflowExecutions( final CountOpenWorkflowExecutionsRequest request ) throws SimpleWorkflowException {
    return request.reply( new WorkflowExecutionCount( ) );
  }

  public PendingTaskCount countPendingActivityTasks( final CountPendingActivityTasksRequest request ) throws SimpleWorkflowException {
    return request.reply( new PendingTaskCount( ) );
  }

  public PendingTaskCount countPendingDecisionTasks( final CountPendingDecisionTasksRequest request ) throws SimpleWorkflowException {
    return request.reply( new PendingTaskCount( ) );
  }

  public WorkflowExecutionInfos listClosedWorkflowExecutions( final ListClosedWorkflowExecutionsRequest request ) throws SimpleWorkflowException {
    return request.reply( new WorkflowExecutionInfos( ) );
  }

  public WorkflowExecutionInfos listOpenWorkflowExecutions( final ListOpenWorkflowExecutionsRequest request ) throws SimpleWorkflowException {
    return request.reply( new WorkflowExecutionInfos( ) );
  }

  public Run startWorkflowExecution( final StartWorkflowExecutionRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowType> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowType.class ).byPrivileges( ).buildPredicate( );
    final WorkflowExecution workflowExecution = allocate( new Supplier<WorkflowExecution>( ) {
      @Override
      public WorkflowExecution get( ) {
        try {
          if ( !workflowExecutions.listByExample(
              WorkflowExecution.exampleForOpenWorkflow( accountFullName, request.getDomain( ), request.getWorkflowId( ) ),
              Predicates.alwaysTrue( ),
              Functions.identity( ) ).isEmpty( ) ) {
            throw new SimpleWorkflowClientException(
                "WorkflowExecutionAlreadyStartedFault", "Workflow open with ID " + request.getWorkflowId( ) );
          }

          final Domain domain =
              domains.lookupByName( accountFullName, request.getDomain( ), Functions.<Domain>identity( ) );
          final WorkflowType workflowType =
              workflowTypes.lookupByExample(
                  WorkflowType.exampleWithUniqueName(
                      accountFullName,
                      request.getDomain( ),
                      request.getWorkflowType( ).getName( ),
                      request.getWorkflowType( ).getVersion( ) ),
                  accountFullName,
                  request.getWorkflowType( ).getName( ),
                  accessible,
                  Functions.<WorkflowType>identity( ) );
          final String childPolicy = Objects.firstNonNull(
              request.getChildPolicy( ),
              workflowType.getDefaultChildPolicy( ) );
          final String taskList = request.getTaskList( ) == null ?
              workflowType.getDefaultTaskList( ):
              request.getTaskList( ).getName( );
          final Integer executionStartToCloseTimeout = Objects.firstNonNull(  //TODO:STEVE: validate not NONE
              parsePeriod( request.getExecutionStartToCloseTimeout( ) ),
              workflowType.getDefaultExecutionStartToCloseTimeout( ) );
          final Integer taskStartToCloseTimeout = Objects.firstNonNull(
              parsePeriod( request.getTaskStartToCloseTimeout( ) ),
              workflowType.getDefaultTaskStartToCloseTimeout( ) );
          final WorkflowExecution workflowExecution = WorkflowExecution.create(
              userFullName,
              UUID.randomUUID( ).toString( ),
              domain,
              workflowType,
              request.getWorkflowId( ),
              childPolicy,
              taskList,
              executionStartToCloseTimeout,
              taskStartToCloseTimeout,
              request.getTagList( ),
              Lists.newArrayList(
                  new WorkflowExecutionStartedEventAttributes( )
                      .withChildPolicy( childPolicy )
                      .withExecutionStartToCloseTimeout( String.valueOf( executionStartToCloseTimeout ) )
                      .withInput( request.getInput( ) )
                      .withParentInitiatedEventId( 0L )
                      .withTaskList( new TaskList( ).withName( taskList ) )
                      .withTagList( request.getTagList( ) )
                      .withTaskStartToCloseTimeout( String.valueOf( taskStartToCloseTimeout ) )
                      .withWorkflowType( request.getWorkflowType( ) ),
                  new DecisionTaskScheduledEventAttributes( )
                      .withStartToCloseTimeout( String.valueOf( taskStartToCloseTimeout ) )
                      .withTaskList( request.getTaskList( ) )
              )
          );
          return workflowExecutions.save( workflowExecution );
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    }, WorkflowExecution.class, request.getWorkflowId( ) );

    longPollExit( );

    final Run run = new Run( );
    run.setRunId( workflowExecution.getDisplayName() );
    return request.reply( run );
  }

  public com.eucalyptus.simpleworkflow.common.model.ActivityTask pollForActivityTask(
      final PollForActivityTaskRequest request
  ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super ActivityTask> accessible =
        SimpleWorkflowMetadatas.filteringFor( ActivityTask.class ).byPrivileges( ).buildPredicate( );

    com.eucalyptus.simpleworkflow.common.model.ActivityTask activityTask = null;
    try {
      final List<ActivityTask> pending = activityTasks.listByExample(
          ActivityTask.examplePending( accountFullName, request.getDomain(), request.getTaskList().getName() ),
          accessible,
          Functions.<ActivityTask>identity( ) );
      Collections.sort( pending, Ordering.natural( ).onResultOf( AbstractPersistentSupport.creation( ) ) );
      for ( final ActivityTask pendingTask : pending ) {
        if ( activityTask != null ) break;
        try {
          activityTask = activityTasks.updateByExample(
              pendingTask,
              accountFullName,
              pendingTask.getDisplayName(),
              new Function<ActivityTask,com.eucalyptus.simpleworkflow.common.model.ActivityTask>(){
                @Nullable
                @Override
                public com.eucalyptus.simpleworkflow.common.model.ActivityTask apply( final ActivityTask activityTask ) {
                  if ( activityTask.getState( ) == ActivityTask.State.Pending ) {
                    final WorkflowExecution workflowExecution = activityTask.getWorkflowExecution( );
                    final Long startedId = workflowExecution.addHistoryEvent(
                        WorkflowHistoryEvent.create( workflowExecution, new ActivityTaskStartedEventAttributes( )
                          .withIdentity( request.getIdentity( ) )
                          .withScheduledEventId( activityTask.getScheduledEventId( ) )
                        )
                    );
                    activityTask.setState( ActivityTask.State.Active );
                    activityTask.setStartedEventId( startedId );
                    return new com.eucalyptus.simpleworkflow.common.model.ActivityTask( )
                        .withStartedEventId( startedId )
                        .withInput( activityTask.getInput() )
                        .withTaskToken( taskTokenManager.encryptTaskToken( new TaskToken(
                            accountFullName.getAccountNumber(),
                            workflowExecution.getDomain().getNaturalId(),
                            workflowExecution.getDisplayName(),
                            activityTask.getScheduledEventId(),
                            startedId,
                            System.currentTimeMillis(),
                            System.currentTimeMillis() ) ) )
                        .withActivityId( activityTask.getDisplayName() )
                        .withActivityType( new com.eucalyptus.simpleworkflow.common.model.ActivityType()
                            .withName( activityTask.getActivityType() )
                            .withVersion( activityTask.getActivityVersion() ) )
                        .withWorkflowExecution( new com.eucalyptus.simpleworkflow.common.model.WorkflowExecution()
                            .withRunId( workflowExecution.getDisplayName() )
                            .withWorkflowId( workflowExecution.getWorkflowId() ) );
                  }
                  return null;
                }
              });

        } catch ( Exception e ) {
          logger.error( "Error taking activity task", e );
        }
      }
    } catch ( Exception e ) {
      throw handleException( e );
    }

    if ( activityTask == null ) {
      longPollSimulation( );
      activityTask = new com.eucalyptus.simpleworkflow.common.model.ActivityTask( );
    }
    return request.reply( activityTask );
  }

  public ActivityTaskStatus recordActivityTaskHeartbeat( final RecordActivityTaskHeartbeatRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super ActivityTask> accessible =
        SimpleWorkflowMetadatas.filteringFor( ActivityTask.class ).byPrivileges( ).buildPredicate( );

    final ActivityTaskStatus status = new ActivityTaskStatus( );
    status.setCancelRequested( false );
    try {
      final TaskToken token =
          taskTokenManager.decryptTaskToken( accountFullName.getAccountNumber( ), request.getTaskToken( ) );

      activityTasks.updateByExample(
          ActivityTask.exampleWithUniqueName( accountFullName, token.getRunId(), token.getScheduledEventId() ),
          accountFullName,
          token.getRunId( ) + "/" + token.getScheduledEventId( ),
          new Function<ActivityTask, ActivityTask>() {
            @Override
            public ActivityTask apply( final ActivityTask activityTask ) {
              if ( accessible.apply( activityTask ) ) {
                activityTask.setHeartbeatDetails( request.getDetails( ) );
                activityTask.updateTimeStamps( );
                status.setCancelRequested( activityTask.getCancelRequestedEventId( ) != null );
              }
              return activityTask;
            }
          } );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          "Unknown activity task, token = " + request.getTaskToken( ) );
    } catch( Exception e ) {
      throw handleException( e );
    }

    return request.reply( status );
  }

  public SimpleWorkflowMessage respondActivityTaskCanceled( final RespondActivityTaskCanceledRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super ActivityTask> accessible =
        SimpleWorkflowMetadatas.filteringFor( ActivityTask.class ).byPrivileges( ).buildPredicate( );

    final ActivityTaskStatus status = new ActivityTaskStatus( );
    status.setCancelRequested( false );
    try {
      final TaskToken token =
          taskTokenManager.decryptTaskToken( accountFullName.getAccountNumber( ), request.getTaskToken( ) );

      activityTasks.updateByExample(
          ActivityTask.exampleWithUniqueName( accountFullName, token.getRunId( ), token.getScheduledEventId( ) ),
          accountFullName,
          token.getRunId( ) + "/" + token.getScheduledEventId( ),
          new Function<ActivityTask, ActivityTask>() {
            @Override
            public ActivityTask apply( final ActivityTask activityTask ) {
              if ( accessible.apply( activityTask ) ) {
                final WorkflowExecution workflowExecution = activityTask.getWorkflowExecution( );
                workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                    workflowExecution,
                    new ActivityTaskCanceledEventAttributes()
                        .withDetails( request.getDetails() )
                        .withLatestCancelRequestedEventId( activityTask.getCancelRequestedEventId() )
                        .withScheduledEventId( activityTask.getScheduledEventId() )
                        .withStartedEventId( activityTask.getStartedEventId() )
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
                }
                Entities.delete( activityTask );
              }
              return activityTask;
            }
          } );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          "Unknown activity task, token = " + request.getTaskToken( ) );
    } catch( Exception e ) {
      throw handleException( e );
    }

    longPollExit( );

    return request.reply( new SimpleWorkflowMessage( ) );

  }

  public SimpleWorkflowMessage respondActivityTaskCompleted( final RespondActivityTaskCompletedRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    try {
      final TaskToken token =
          taskTokenManager.decryptTaskToken( accountFullName.getAccountNumber( ), request.getTaskToken( ) );
      final Domain domain = domains.lookupByExample(
          Domain.exampleWithUuid( accountFullName, token.getDomainUuid( ) ),
          accountFullName,
          token.getDomainUuid( ),
          Predicates.alwaysTrue( ),
          Functions.<Domain>identity( ) );

      workflowExecutions.updateByExample(
          WorkflowExecution.exampleWithUniqueName( accountFullName, domain.getDisplayName( ), token.getRunId( ) ),
          accountFullName,
          token.getRunId( ),
          new Function<WorkflowExecution, WorkflowExecution>() {
            @Nullable
            @Override
            public WorkflowExecution apply( final WorkflowExecution workflowExecution ) {
              if ( accessible.apply( workflowExecution ) ) {
                try {
                  activityTasks.deleteByExample( ActivityTask.exampleWithUniqueName(
                      accountFullName,
                      token.getRunId( ),
                      token.getScheduledEventId( ) ) );
                } catch ( SwfMetadataException e ) {
                  throw Exceptions.toUndeclared( e );
                }

                // TODO:STEVE: verify token valid (no reuse, etc)
                workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                    workflowExecution,
                    new ActivityTaskCompletedEventAttributes( )
                        .withResult( request.getResult( ) )
                        .withScheduledEventId( token.getScheduledEventId( ) )
                        .withStartedEventId( token.getStartedEventId( ) )
                ) );
                workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                    workflowExecution,
                    new DecisionTaskScheduledEventAttributes()
                        .withTaskList( new TaskList( ).withName( workflowExecution.getTaskList( ) ) )
                        .withStartToCloseTimeout( String.valueOf( workflowExecution.getTaskStartToCloseTimeout( ) ) )
                ) );
                workflowExecution.setDecisionStatus( Pending );
                workflowExecution.setDecisionTimestamp( new Date( ) );
              }
              return workflowExecution;
            }
          } );
    } catch( Exception e ) {
      throw handleException( e );
    }

    longPollExit( );

    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public SimpleWorkflowMessage respondActivityTaskFailed( final RespondActivityTaskFailedRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    try {
      final TaskToken token =
          taskTokenManager.decryptTaskToken( accountFullName.getAccountNumber( ), request.getTaskToken( ) );
      final Domain domain = domains.lookupByExample(
          Domain.exampleWithUuid( accountFullName, token.getDomainUuid( ) ),
          accountFullName,
          token.getDomainUuid( ),
          Predicates.alwaysTrue( ),
          Functions.<Domain>identity( ) );

      workflowExecutions.updateByExample(
          WorkflowExecution.exampleWithUniqueName( accountFullName, domain.getDisplayName( ), token.getRunId( ) ),
          accountFullName,
          token.getRunId( ),
          new Function<WorkflowExecution, WorkflowExecution>() {
            @Nullable
            @Override
            public WorkflowExecution apply( final WorkflowExecution workflowExecution ) {
              if ( accessible.apply( workflowExecution ) ) {
                try {
                  activityTasks.deleteByExample( ActivityTask.exampleWithUniqueName(
                      accountFullName,
                      token.getRunId( ),
                      token.getScheduledEventId( ) ) );
                } catch ( SwfMetadataException e ) {
                  throw Exceptions.toUndeclared( e );
                }

                // TODO:STEVE: verify token valid (no reuse, etc)
                workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                    workflowExecution,
                    new ActivityTaskFailedEventAttributes()
                        .withDetails( request.getDetails() )
                        .withReason( request.getReason() )
                        .withScheduledEventId( token.getScheduledEventId() )
                        .withStartedEventId( token.getStartedEventId() )
                ) );
                workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                    workflowExecution,
                    new DecisionTaskScheduledEventAttributes()
                        .withTaskList( new TaskList( ).withName( workflowExecution.getTaskList( ) ) )
                        .withStartToCloseTimeout( String.valueOf( workflowExecution.getTaskStartToCloseTimeout( ) ) )
                ) );
                workflowExecution.setDecisionStatus( Pending );
                workflowExecution.setDecisionTimestamp( new Date( ) );
              }
              return workflowExecution;
            }
          } );
    } catch( Exception e ) {
      throw handleException( e );
    }

    longPollExit( );

    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public DecisionTask pollForDecisionTask( final PollForDecisionTaskRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    DecisionTask decisionTask = null;
    try {
      final List<WorkflowExecution> pending = workflowExecutions.listByExample(
          WorkflowExecution.exampleWithPendingDecision( accountFullName, request.getDomain( ), request.getTaskList( ).getName( ) ),
          accessible,
          Functions.<WorkflowExecution>identity( ) );
      Collections.sort( pending, Ordering.natural( ).onResultOf( AbstractPersistentSupport.creation( ) ) );
      for ( final WorkflowExecution execution : pending ) {
        if ( decisionTask != null ) break;
        try {
          decisionTask = workflowExecutions.updateByExample(
              WorkflowExecution.exampleWithUniqueName( accountFullName, execution.getDomainName( ), execution.getDisplayName( ) ),
              accountFullName,
              execution.getDisplayName(),
              new Function<WorkflowExecution,DecisionTask>( ) {
                @Nullable
                @Override
                public DecisionTask apply( final WorkflowExecution workflowExecution ) {
                  if ( workflowExecution.getDecisionStatus( ) == Pending ) {
                    final List<WorkflowHistoryEvent> events = workflowExecution.getWorkflowHistory();
                    final List<WorkflowHistoryEvent> reverseEvents = Lists.reverse( events );
                    final WorkflowHistoryEvent scheduled = Iterables.find(
                        reverseEvents,
                        CollectionUtils.propertyPredicate( "DecisionTaskScheduled", WorkflowExecutions.WorkflowHistoryEventStringFunctions.EVENT_TYPE ) );
                    final Optional<WorkflowHistoryEvent> previousStarted = Iterables.tryFind(
                        reverseEvents,
                        CollectionUtils.propertyPredicate( "DecisionTaskStarted", WorkflowExecutions.WorkflowHistoryEventStringFunctions.EVENT_TYPE ) );
                    workflowExecution.setDecisionStatus( Active );
                    workflowExecution.setDecisionTimestamp( new Date( ) );
                    final WorkflowHistoryEvent started = WorkflowHistoryEvent.create(
                        workflowExecution,
                        new DecisionTaskStartedEventAttributes()
                            .withIdentity( request.getIdentity() )
                            .withScheduledEventId( scheduled.getEventId() ) );
                    workflowExecution.addHistoryEvent( started );
                    return new DecisionTask( )
                        .withWorkflowExecution( new com.eucalyptus.simpleworkflow.common.model.WorkflowExecution( )
                            .withWorkflowId( workflowExecution.getWorkflowId( ) )
                            .withRunId( workflowExecution.getDisplayName( ) ) )
                        .withWorkflowType( new com.eucalyptus.simpleworkflow.common.model.WorkflowType()
                            .withName( workflowExecution.getWorkflowType( ).getDisplayName( ) )
                            .withVersion( workflowExecution.getWorkflowType( ).getWorkflowVersion( ) ) )
                        .withTaskToken( taskTokenManager.encryptTaskToken( new TaskToken(
                            accountFullName.getAccountNumber( ),
                            workflowExecution.getDomain( ).getNaturalId( ),
                            workflowExecution.getDisplayName( ),
                            scheduled.getEventId( ),
                            started.getEventId( ),
                            System.currentTimeMillis( ),
                            System.currentTimeMillis( ) ) ) )  //TODO:STEVE: token expiry date
                        .withStartedEventId( started.getEventId() )
                        .withPreviousStartedEventId( previousStarted.transform( WorkflowExecutions.WorkflowHistoryEventLongFunctions.EVENT_ID ).or( 0L ) )
                        .withEvents( Collections2.transform(
                            Objects.firstNonNull( request.isReverseOrder( ), Boolean.FALSE ) ? reverseEvents : events,
                            TypeMappers.lookup( WorkflowHistoryEvent.class, HistoryEvent.class )
                        ) );
                  }
                  return null;
                }
              } );
        } catch ( Exception e ) {
          logger.error( "Error taking decision task", e );
        }
      }
    } catch ( Exception e ) {
      throw handleException( e );
    }

    if ( decisionTask == null ) {
      longPollSimulation( );
      decisionTask = new DecisionTask( );
    }
    return request.reply( decisionTask );
  }

  public SimpleWorkflowMessage respondDecisionTaskCompleted( final RespondDecisionTaskCompletedRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    try {
      final TaskToken token =
          taskTokenManager.decryptTaskToken( accountFullName.getAccountNumber(), request.getTaskToken() );
      final Domain domain = domains.lookupByExample(
          Domain.exampleWithUuid( accountFullName, token.getDomainUuid( ) ),
          accountFullName,
          token.getDomainUuid( ),
          Predicates.alwaysTrue( ),
          Functions.<Domain>identity( ) );

      workflowExecutions.updateByExample(
          WorkflowExecution.exampleWithUniqueName( accountFullName, domain.getDisplayName( ), token.getRunId( ) ),
          accountFullName,
          token.getRunId( ),
          new Function<WorkflowExecution, WorkflowExecution>() {
            @Nullable
            @Override
            public WorkflowExecution apply( final WorkflowExecution workflowExecution ) {
              if ( accessible.apply( workflowExecution ) ) {
                // verify token is valid
                final List<WorkflowHistoryEvent> events = workflowExecution.getWorkflowHistory();
                final List<WorkflowHistoryEvent> reverseEvents = Lists.reverse( events );
                final WorkflowHistoryEvent started = Iterables.find(
                    reverseEvents,
                    CollectionUtils.propertyPredicate( "DecisionTaskStarted", WorkflowExecutions.WorkflowHistoryEventStringFunctions.EVENT_TYPE ) );
                if ( !started.getEventId( ).equals( token.getStartedEventId( ) ) ) {
                  throw Exceptions.toUndeclared( new SimpleWorkflowClientException( "ValidationError", "Bad token" ) );
                }
                final WorkflowHistoryEvent scheduled = Iterables.find(
                    reverseEvents,
                    CollectionUtils.propertyPredicate( "DecisionTaskScheduled", WorkflowExecutions.WorkflowHistoryEventStringFunctions.EVENT_TYPE ) );
                if ( scheduled.getEventId( ) < started.getEventId() ) {
                  workflowExecution.setDecisionStatus( Idle );
                  workflowExecution.setDecisionTimestamp( new Date( ) );
                } else {
                  workflowExecution.setDecisionStatus( Pending );
                  workflowExecution.setDecisionTimestamp( new Date( ) );
                }

                // process decision task response
                workflowExecution.setLatestExecutionContext( request.getExecutionContext( ) );
                final Long completedId = workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                    workflowExecution,
                    new DecisionTaskCompletedEventAttributes( )
                        .withExecutionContext( request.getExecutionContext( ) )
                        .withScheduledEventId( token.getScheduledEventId( ) )
                        .withStartedEventId( token.getStartedEventId( ) )
                ) );
                boolean scheduleDecisionTask = false;
                if ( request.getDecisions( ) != null ) for ( final Decision decision : request.getDecisions() ) {
                  switch ( decision.getDecisionType( ) ) {
                    case "CancelTimer":
                      final CancelTimerDecisionAttributes cancelTimer = decision.getCancelTimerDecisionAttributes( );
                      workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                          workflowExecution,
                          new CancelTimerFailedEventAttributes()
                              .withCause( CancelTimerFailedCause.TIMER_ID_UNKNOWN )
                              .withDecisionTaskCompletedEventId( completedId )
                              .withTimerId( cancelTimer.getTimerId( ) )
                      ) );
                      scheduleDecisionTask = true;
                      break;
                    case "CancelWorkflowExecution":
                      final CancelWorkflowExecutionDecisionAttributes cancelWorkflowExecution =
                          decision.getCancelWorkflowExecutionDecisionAttributes();
                      workflowExecution.setState( WorkflowExecution.ExecutionStatus.Closed );
                      workflowExecution.setCloseStatus( WorkflowExecution.CloseStatus.Canceled );
                      workflowExecution.setCloseTimestamp( new Date( ) );
                      workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                          workflowExecution,
                          new WorkflowExecutionCanceledEventAttributes( )
                              .withDecisionTaskCompletedEventId( completedId )
                              .withDetails( cancelWorkflowExecution.getDetails() )
                      ) );
                      deleteActivities( activityTasks, accountFullName, workflowExecution );
                      break;
                    case "CompleteWorkflowExecution":
                      final CompleteWorkflowExecutionDecisionAttributes completed =
                          decision.getCompleteWorkflowExecutionDecisionAttributes( );
                      workflowExecution.setState( WorkflowExecution.ExecutionStatus.Closed );
                      workflowExecution.setCloseStatus( WorkflowExecution.CloseStatus.Completed );
                      workflowExecution.setCloseTimestamp( new Date( ) );
                      workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                          workflowExecution,
                          new WorkflowExecutionCompletedEventAttributes( )
                            .withDecisionTaskCompletedEventId( completedId )
                            .withResult( completed.getResult( ) )
                      ) );
                      break;
                    case "ContinueAsNewWorkflowExecution":
                      workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                          workflowExecution,
                          new ContinueAsNewWorkflowExecutionFailedEventAttributes()
                              .withCause( ContinueAsNewWorkflowExecutionFailedCause.OPERATION_NOT_PERMITTED )
                              .withDecisionTaskCompletedEventId( completedId )
                      ) );
                      scheduleDecisionTask = true;
                      break;
                    case "FailWorkflowExecution":
                      final FailWorkflowExecutionDecisionAttributes failed =
                          decision.getFailWorkflowExecutionDecisionAttributes();
                      workflowExecution.setState( WorkflowExecution.ExecutionStatus.Closed );
                      workflowExecution.setCloseStatus( WorkflowExecution.CloseStatus.Failed );
                      workflowExecution.setCloseTimestamp( new Date( ) );
                      workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                          workflowExecution,
                          new WorkflowExecutionFailedEventAttributes( )
                              .withDecisionTaskCompletedEventId( completedId )
                              .withDetails( failed.getDetails( ) )
                              .withReason( failed.getReason( ) ) ) );
                      deleteActivities( activityTasks, accountFullName, workflowExecution );
                      break;
                    case "RecordMarker":
                      final RecordMarkerDecisionAttributes mark = decision.getRecordMarkerDecisionAttributes( );
                      workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                          workflowExecution,
                          new MarkerRecordedEventAttributes( )
                              .withDetails( mark.getDetails( ) )
                              .withDecisionTaskCompletedEventId( completedId )
                              .withMarkerName( mark.getMarkerName( ) )
                      ) );
                      break;
                    case "RequestCancelActivityTask":
                      final RequestCancelActivityTaskDecisionAttributes cancelActivity =
                          decision.getRequestCancelActivityTaskDecisionAttributes();
                      try {
                        activityTasks.updateByExample(
                            ActivityTask.exampleWithActivityId(
                                accountFullName,
                                workflowExecution.getDomainName( ),
                                workflowExecution.getDisplayName( ),
                                cancelActivity.getActivityId( ) ),
                            accountFullName,
                            cancelActivity.getActivityId( ),
                            new Function<ActivityTask, Void>() {
                              @Override
                              public Void apply( final ActivityTask activityTask ) {
                                final Long cancelRequestedId = workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                                    workflowExecution,
                                    new ActivityTaskCancelRequestedEventAttributes()
                                        .withDecisionTaskCompletedEventId( completedId )
                                        .withActivityId( cancelActivity.getActivityId() )
                                ) );

                                if ( activityTask.getState( ) == ActivityTask.State.Active ) {
                                  activityTask.setCancelRequestedEventId( cancelRequestedId );
                                } else {
                                  workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                                      workflowExecution,
                                      new ActivityTaskCanceledEventAttributes()
                                          .withLatestCancelRequestedEventId( cancelRequestedId )
                                          .withScheduledEventId( activityTask.getScheduledEventId() )
                                          .withStartedEventId( activityTask.getStartedEventId() )
                                  ) );
                                  Entities.delete( activityTask );
                                }
                                return null;
                              }
                            }
                        );
                      } catch ( SwfMetadataNotFoundException e ) {
                        workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                            workflowExecution,
                            new RequestCancelActivityTaskFailedEventAttributes( )
                                .withCause( RequestCancelActivityTaskFailedCause.ACTIVITY_ID_UNKNOWN )
                                .withDecisionTaskCompletedEventId( completedId )
                                .withActivityId( cancelActivity.getActivityId( ) )
                        ) );
                      } catch ( SwfMetadataException e ) {
                        throw Exceptions.toUndeclared( e );
                      }
                      scheduleDecisionTask = true;
                      break;
                    case "RequestCancelExternalWorkflowExecution":
                      final RequestCancelExternalWorkflowExecutionDecisionAttributes cancelExternalWorkflow =
                          decision.getRequestCancelExternalWorkflowExecutionDecisionAttributes();
                      workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                          workflowExecution,
                          new RequestCancelExternalWorkflowExecutionFailedEventAttributes()
                              .withCause( RequestCancelExternalWorkflowExecutionFailedCause.UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION )
                              .withControl( cancelExternalWorkflow.getControl() )
                              .withDecisionTaskCompletedEventId( completedId )
                              .withRunId( cancelExternalWorkflow.getRunId() )
                              .withWorkflowId( cancelExternalWorkflow.getWorkflowId() )
                      ) );
                      scheduleDecisionTask = true;
                      break;
                    case "ScheduleActivityTask":
                      workflowExecution.setLatestActivityTaskScheduled( new Date( ) );
                      final ScheduleActivityTaskDecisionAttributes scheduleActivity =
                          decision.getScheduleActivityTaskDecisionAttributes();
                      final Long scheduledId = workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                          workflowExecution,
                          new ActivityTaskScheduledEventAttributes( )
                              .withDecisionTaskCompletedEventId( completedId )
                              .withActivityId( scheduleActivity.getActivityId( ) )
                              .withActivityType( scheduleActivity.getActivityType( ) )
                              .withControl( scheduleActivity.getControl( ) )
                              .withHeartbeatTimeout( scheduleActivity.getHeartbeatTimeout( ) )
                              .withInput( scheduleActivity.getInput( ) )
                              .withScheduleToCloseTimeout( scheduleActivity.getScheduleToCloseTimeout( ) )
                              .withScheduleToStartTimeout( scheduleActivity.getScheduleToStartTimeout( ) )
                              .withStartToCloseTimeout( scheduleActivity.getStartToCloseTimeout( ) )
                              .withTaskList( scheduleActivity.getTaskList( ) )
                      ) );
                      try {
                        final ActivityType activityType = activityTypes.lookupByExample(
                            ActivityType.exampleWithUniqueName(
                                accountFullName,
                                domain.getDisplayName( ),
                                scheduleActivity.getActivityType( ).getName( ),
                                scheduleActivity.getActivityType( ).getVersion( ) ),
                            accountFullName,
                            scheduleActivity.getActivityType( ).getName( ),
                            Predicates.alwaysTrue( ),
                            Functions.<ActivityType>identity( ) );

                        activityTasks.save( com.eucalyptus.simpleworkflow.ActivityTask.create(
                            userFullName,
                            workflowExecution,
                            domain.getDisplayName(),
                            scheduleActivity.getActivityId(),
                            scheduleActivity.getActivityType().getName(),
                            scheduleActivity.getActivityType().getVersion(),
                            scheduleActivity.getInput(),
                            scheduledId,
                            scheduleActivity.getTaskList() == null ?
                                activityType.getDefaultTaskList() :
                                scheduleActivity.getTaskList().getName(),
                            parsePeriod( scheduleActivity.getScheduleToCloseTimeout( ), activityType.getDefaultTaskScheduleToCloseTimeout( ) ),
                            parsePeriod( scheduleActivity.getScheduleToStartTimeout( ), activityType.getDefaultTaskScheduleToStartTimeout( ) ),
                            parsePeriod( scheduleActivity.getStartToCloseTimeout( ), activityType.getDefaultTaskStartToCloseTimeout( ) ),
                            parsePeriod( scheduleActivity.getHeartbeatTimeout( ), activityType.getDefaultTaskHeartbeatTimeout( ) )
                        ) );
                      } catch ( Exception e ) {
                        throw Exceptions.toUndeclared( e );
                      }
                      break;
                    case "SignalExternalWorkflowExecution":
                      final SignalExternalWorkflowExecutionDecisionAttributes signalExternalWorkflow =
                          decision.getSignalExternalWorkflowExecutionDecisionAttributes();
                      workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                          workflowExecution,
                          new SignalExternalWorkflowExecutionFailedEventAttributes( )
                              .withCause( SignalExternalWorkflowExecutionFailedCause.UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION )
                              .withControl( signalExternalWorkflow.getControl() )
                              .withDecisionTaskCompletedEventId( completedId )
                              .withRunId( signalExternalWorkflow.getRunId( ) )
                              .withWorkflowId( signalExternalWorkflow.getWorkflowId( ) )
                      ) );
                      scheduleDecisionTask = true;
                      break;
                    case "StartChildWorkflowExecution":
                      final StartChildWorkflowExecutionDecisionAttributes startChildWorkflow =
                          decision.getStartChildWorkflowExecutionDecisionAttributes();
                      workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                          workflowExecution,
                          new StartChildWorkflowExecutionFailedEventAttributes()
                              .withCause( StartChildWorkflowExecutionFailedCause.OPERATION_NOT_PERMITTED )
                              .withControl( startChildWorkflow.getControl() )
                              .withDecisionTaskCompletedEventId( completedId )
                              .withWorkflowId( startChildWorkflow.getWorkflowId() )
                              .withWorkflowType( startChildWorkflow.getWorkflowType() )
                      ) );
                      scheduleDecisionTask = true;
                      break;
                    case "StartTimer":
                      final StartTimerDecisionAttributes startTimer = decision.getStartTimerDecisionAttributes();
                      workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                          workflowExecution,
                          new StartTimerFailedEventAttributes()
                              .withCause( StartTimerFailedCause.OPERATION_NOT_PERMITTED )
                              .withDecisionTaskCompletedEventId( completedId )
                              .withTimerId( startTimer.getTimerId() )
                      ) );
                      scheduleDecisionTask = true;
                      break;
                    default:
                      throw Exceptions.toUndeclared( new SimpleWorkflowException(
                          "InternalFailure",
                          Role.Receiver,
                          "Unsupported decision type: " + decision.getDecisionType( ) ) );
                  }
                }
                if ( scheduleDecisionTask && workflowExecution.getDecisionStatus( ) != Pending ) {
                  workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                      workflowExecution,
                      new DecisionTaskScheduledEventAttributes()
                          .withTaskList( new TaskList( ).withName( workflowExecution.getTaskList( ) ) )
                          .withStartToCloseTimeout( String.valueOf( workflowExecution.getTaskStartToCloseTimeout( ) ) )
                  ) );
                  workflowExecution.setDecisionStatus( Pending );
                  workflowExecution.setDecisionTimestamp( new Date( ) );
                }
              }
              return workflowExecution;
            }
          } );
    } catch( Exception e ) {
      throw handleException( e );
    }

    longPollExit( );

    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public SimpleWorkflowMessage signalWorkflowExecution( final SignalWorkflowExecutionRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    try {
      workflowExecutions.updateByExample(
          WorkflowExecution.exampleForOpenWorkflow( accountFullName, request.getDomain( ), request.getWorkflowId( ) ),
          accountFullName,
          request.getWorkflowId( ),
          new Function<WorkflowExecution,Void>(){
            @Override
            public Void apply( final WorkflowExecution workflowExecution ) {
              if ( accessible.apply( workflowExecution ) ) {
                workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                        workflowExecution,
                        new WorkflowExecutionSignaledEventAttributes( )
                            .withExternalInitiatedEventId( 0L )
                            .withInput( request.getInput( ) )
                            .withSignalName( request.getSignalName( ) )
                ) );
                if ( workflowExecution.getDecisionStatus( ) != Pending ) {
                  workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                      workflowExecution,
                      new DecisionTaskScheduledEventAttributes()
                          .withTaskList( new TaskList( ).withName( workflowExecution.getTaskList( ) ) )
                          .withStartToCloseTimeout( String.valueOf( workflowExecution.getTaskStartToCloseTimeout( ) ) )
                  ) );
                  workflowExecution.setDecisionStatus( Pending );
                  workflowExecution.setDecisionTimestamp( new Date( ) );
                }
              }
              return null;
            }
          }
      );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          request.getRunId( ) == null ?
              "Unknown execution, workflowId = " + request.getWorkflowId( ) :
              "Unknown execution: WorkflowExecution=[workflowId=" + request.getWorkflowId( ) + ", runId="+ request.getRunId( ) +"]" );
    } catch ( SwfMetadataException e ) {
      throw handleException( e );
    }

    longPollExit( );

    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public SimpleWorkflowMessage requestCancelWorkflowExecution( final RequestCancelWorkflowExecutionRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    try {
      workflowExecutions.updateByExample(
          WorkflowExecution.exampleForOpenWorkflow( accountFullName, request.getDomain( ), request.getWorkflowId( ) ),
          accountFullName,
          request.getWorkflowId( ),
          new Function<WorkflowExecution,Void>(){
            @Override
            public Void apply( final WorkflowExecution workflowExecution ) {
              if ( accessible.apply( workflowExecution ) ) {
                workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                    workflowExecution,
                    new WorkflowExecutionCancelRequestedEventAttributes( )
                        .withExternalInitiatedEventId( 0L )
                ) );
                if ( workflowExecution.getDecisionStatus( ) != Pending ) {
                  workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                      workflowExecution,
                      new DecisionTaskScheduledEventAttributes()
                          .withTaskList( new TaskList( ).withName( workflowExecution.getTaskList( ) ) )
                          .withStartToCloseTimeout( String.valueOf( workflowExecution.getTaskStartToCloseTimeout( ) ) )
                  ) );
                  workflowExecution.setDecisionStatus( Pending );
                  workflowExecution.setDecisionTimestamp( new Date( ) );
                }
              }
              return null;
            }
          }
      );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          request.getRunId( ) == null ?
              "Unknown execution, workflowId = " + request.getWorkflowId( ) :
              "Unknown execution: WorkflowExecution=[workflowId=" + request.getWorkflowId( ) + ", runId="+ request.getRunId( ) +"]" );
    } catch ( SwfMetadataException e ) {
      throw handleException( e );
    }

    longPollExit( );

    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public SimpleWorkflowMessage terminateWorkflowExecution( final TerminateWorkflowExecutionRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    try {
      workflowExecutions.updateByExample(
          WorkflowExecution.exampleForOpenWorkflow( accountFullName, request.getDomain( ), request.getWorkflowId( ) ),
          accountFullName,
          request.getWorkflowId( ),
           new Function<WorkflowExecution,Void>(){
              @Override
              public Void apply( final WorkflowExecution workflowExecution ) {
                if ( accessible.apply( workflowExecution ) ) {
                  workflowExecution.setState( WorkflowExecution.ExecutionStatus.Closed );
                  workflowExecution.setCloseStatus( WorkflowExecution.CloseStatus.Terminated );
                  workflowExecution.setCloseTimestamp( new Date( ) );
                  workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                          workflowExecution,
                          new WorkflowExecutionTerminatedEventAttributes()
                              .withChildPolicy( Objects.firstNonNull(
                                  request.getChildPolicy( ),
                                  workflowExecution.getChildPolicy( ) ) )
                              .withDetails( request.getDetails() )
                              .withReason( request.getReason() ) )
                  );
                }
                deleteActivities( activityTasks, accountFullName, workflowExecution );
                return null;
             }
           }
      );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          request.getRunId( ) == null ?
          "Unknown execution, workflowId = " + request.getWorkflowId( ) :
          "Unknown execution: WorkflowExecution=[workflowId=" + request.getWorkflowId( ) + ", runId="+ request.getRunId( ) +"]" );
    } catch ( SwfMetadataException e ) {
      throw handleException( e );
    }

    longPollExit( );

    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public History getWorkflowExecutionHistory( final GetWorkflowExecutionHistoryRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    final History history;
    try {
      history = workflowExecutions.lookupByExample(
          WorkflowExecution.exampleWithName( accountFullName, request.getExecution().getRunId( ) ),
          accountFullName,
          request.getExecution().getRunId(),
          accessible,
          new Function<WorkflowExecution, History>() {
            @Override
            public History apply( final WorkflowExecution workflowExecution ) {
              final List<WorkflowHistoryEvent> events = workflowExecution.getWorkflowHistory();
              final List<WorkflowHistoryEvent> reverseEvents = Lists.reverse( events );
              return new History( )
                  .withEvents( Collections2.transform(
                      Objects.firstNonNull( request.isReverseOrder( ), Boolean.FALSE ) ? reverseEvents : events,
                      TypeMappers.lookup( WorkflowHistoryEvent.class, HistoryEvent.class )
                  ) );
            }
          }
      );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          "Unknown execution, runId = " + request.getExecution().getRunId( ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }

    return request.reply( history );
  }

  private <T extends AbstractPersistent & RestrictedType> T allocate(
      final Supplier<T> allocator,
      final Class<T> type,
      final String name
  ) throws SimpleWorkflowException {
    try {
      return RestrictedTypes.allocateUnitlessResources( type, 1, transactional( allocator ) ).get( 0 );
    } catch ( Exception e ) {
      final SQLException sqlException = Exceptions.findCause( e, SQLException.class ); //TODO:STEVE: why no ConstraintViolationException?
      final ConstraintViolationException constraintViolationException =
          Exceptions.findCause( e, ConstraintViolationException.class );
      if ( constraintViolationException != null || ( sqlException != null && "23505".equals( sqlException.getSQLState( ) ) ) ) {
        final String typeName = type.getSimpleName( );
        final String faultPrefix = typeName.endsWith( "Type" ) ? "Type" : typeName;
        throw new SimpleWorkflowClientException( faultPrefix + "AlreadyExistsFault", typeName + " already exists: " + name );
      }
      throw handleException( e );
    }
  }

  protected <E extends AbstractPersistent> Supplier<E> transactional( final Supplier<E> supplier ) {
    return Entities.asTransaction( supplier );
  }

  private static void deleteActivities( final ActivityTasks activityTasks,
                                        final AccountFullName accountFullName,
                                        final WorkflowExecution workflowExecution ) {
    try {
      activityTasks.deleteByExample(
          ActivityTask.exampleWithWorkflowExecution(
              accountFullName,
              workflowExecution.getDomainName( ),
              workflowExecution.getDisplayName( ) ) );
    } catch ( SwfMetadataException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  /**
   * Method always throws, signature allows use of "throw handleException ..."
   */
  private static SimpleWorkflowException handleException( final Exception e ) throws SimpleWorkflowException {
    final SimpleWorkflowException cause = Exceptions.findCause( e, SimpleWorkflowException.class );
    if ( cause != null ) {
      throw cause;
    }

    final AuthQuotaException quotaCause = Exceptions.findCause( e, AuthQuotaException.class );
    if ( quotaCause != null ) {
      throw new SimpleWorkflowClientException( "ResourceLimitExceeded", "Request would exceed quota for type: " + quotaCause.getType( ) );
    }

    final TaskTokenException tokenCause = Exceptions.findCause( e, TaskTokenException.class );
    if ( tokenCause != null ) {
      throw new SimpleWorkflowClientException( "InvalidParameterValue", "Invalid task token." );
    }

    logger.error( e, e );

    final SimpleWorkflowException exception = new SimpleWorkflowException( "InternalError", Role.Receiver, String.valueOf(e.getMessage( )) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
      exception.initCause( e );
    }
    throw exception;
  }

  private Integer parsePeriod( final String period ) throws SimpleWorkflowException {
    if ( period == null || "NONE".equals( period ) ) {
      return null;
    } else {
      return Integer.parseInt( period );
    }
  }

  private Integer parsePeriod( final String period, final Integer defaultValue ) throws SimpleWorkflowException {
    if ( period == null ) {
      return defaultValue;
    } else if ( "NONE".equals( period ) ) {
      return null;
    } else {
      return Integer.parseInt( period );
    }
  }

  private static void longPollExit( ) {
    synchronized ( pollNotifier ) {
      pollNotifier.notifyAll( );
    }
  }

  private static void longPollSimulation( ) {
    try {
      synchronized ( pollNotifier ) {
        pollNotifier.wait( 5000L );
      }
    } catch ( InterruptedException e ) {
      // respond immediately
    }
  }
}
