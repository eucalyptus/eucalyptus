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
package com.eucalyptus.simpleworkflow;

import static com.eucalyptus.simpleworkflow.Domain.Status.Registered;
import static com.eucalyptus.simpleworkflow.WorkflowExecution.DecisionStatus.*;
import static com.eucalyptus.simpleworkflow.WorkflowExecution.WorkflowHistorySizeLimitException;
import static com.eucalyptus.simpleworkflow.WorkflowExecutions.WorkflowHistoryEventStringFunctions.EVENT_TYPE;
import static com.eucalyptus.simpleworkflow.common.model.ScheduleActivityTaskFailedCause.*;
import java.lang.System;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.criterion.Conjunction;
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
import com.eucalyptus.entities.PersistenceExceptions;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.simpleworkflow.NotifyClient.NotifyTaskList;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadatas;
import com.eucalyptus.simpleworkflow.common.model.*;
import com.eucalyptus.simpleworkflow.tokens.TaskToken;
import com.eucalyptus.simpleworkflow.tokens.TaskTokenException;
import com.eucalyptus.simpleworkflow.tokens.TaskTokenManager;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Pair;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.ws.Role;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

/**
 *
 */
@SuppressWarnings( { "UnusedDeclaration", "Guava", "StaticPseudoFunctionalStyleMethod", "ThrowableResultOfMethodCallIgnored", "Convert2Lambda" } )
@ComponentNamed
public class SimpleWorkflowService {

  private static final Logger logger = Logger.getLogger( SimpleWorkflowService.class );
  private static final ConcurrentMap<NotifyTaskList, Long> taskListActivity = Maps.newConcurrentMap( );

  private final Domains domains;
  private final ActivityTasks activityTasks;
  private final ActivityTypes activityTypes;
  private final WorkflowTypes workflowTypes;
  private final WorkflowExecutions workflowExecutions;
  private final TaskTokenManager taskTokenManager;
  private final Timers timers;

  @Inject
  public SimpleWorkflowService( final Domains domains,
                                final ActivityTasks activityTasks,
                                final ActivityTypes activityTypes,
                                final WorkflowTypes workflowTypes,
                                final WorkflowExecutions workflowExecutions,
                                final TaskTokenManager taskTokenManager,
                                final Timers timers ) {
    this.domains = domains;
    this.activityTasks = activityTasks;
    this.activityTypes = activityTypes;
    this.workflowTypes = workflowTypes;
    this.workflowExecutions = workflowExecutions;
    this.taskTokenManager = taskTokenManager;
    this.timers = timers;
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
              MoreObjects.firstNonNull( parsePeriod( request.getWorkflowExecutionRetentionPeriodInDays( ), 0 ), 0 ) );
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
      domains.withRetries( ).updateByExample(
          Domain.exampleWithName( accountFullName, request.getName( ) ),
          accountFullName,
          request.getName( ),
          domain -> {
            if ( accessible.apply( domain ) ) try {
              if  ( domain.getState( ) == Domain.Status.Deprecated ) {
                throw upClient( "DomainDeprecatedFault", "Domain already deprecated: " + request.getName() );
              }

              domain.setState( Domain.Status.Deprecated );

              activityTypes.list( // transform modifies state
                  accountFullName,
                  CollectionUtils.propertyPredicate( domain.getDisplayName( ), ActivityTypes.StringFunctions.DOMAIN ),
                  ActivityType.Status.Deprecated.set( ) );

              workflowTypes.list( // transform modifies state
                  accountFullName,
                  CollectionUtils.propertyPredicate( domain.getDisplayName( ), WorkflowTypes.StringFunctions.DOMAIN ),
                  WorkflowType.Status.Deprecated.set( ) );
            } catch ( final Exception e ) {
              throw up( e );
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
          Collections.emptyMap( ),
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
              domains.lookupByName( accountFullName, request.getDomain( ),  Registered, Functions.identity( ) );
          if ( activityTypes.countByDomain( accountFullName, domain.getDisplayName( ) ) >=
              SimpleWorkflowProperties.getActivityTypesPerDomain() ) {
            throw upClient( "LimitExceededFault", "Request would exceed limit for type: activity-type" );
          }
          final ActivityType activityType = ActivityType.create(
              userFullName,
              request.getName( ),
              request.getVersion( ),
              domain,
              request.getDescription( ),
              request.getDefaultTaskList( ) == null ? null : request.getDefaultTaskList( ).getName( ),
              parsePeriod( request.getDefaultTaskHeartbeatTimeout( ), -1 ),
              parsePeriod( request.getDefaultTaskScheduleToCloseTimeout( ), -1 ),
              parsePeriod( request.getDefaultTaskScheduleToStartTimeout( ), -1 ),
              parsePeriod( request.getDefaultTaskStartToCloseTimeout( ), -1 ),
              parsePeriod( request.getDefaultTaskPriority( ), 0)
          );
          return activityTypes.save( activityType );
        } catch ( Exception ex ) {
          throw up( ex );
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
          activityType -> {
            if ( accessible.apply( activityType ) ) {
              if ( activityType.getState( ) == ActivityType.Status.Deprecated ) {
                throw upClient(
                    "TypeDeprecatedFault",
                    "Activity type already deprecated: " + request.getActivityType().getName() );
              }
              activityType.setState( ActivityType.Status.Deprecated );
              activityType.setDeprecationTimestamp( new Date( ) );
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
          requestedAndAccessible,
          TypeMappers.lookup( ActivityType.class, ActivityTypeInfo.class ) ) );
      final Ordering<ActivityTypeInfo> ordering =
          Ordering.natural( ).onResultOf( ActivityTypes.ActivityTypeInfoStringFunctions.NAME );
      Collections.sort(
          activityTypeInfos.getTypeInfos( ),
          MoreObjects.firstNonNull( request.getReverseOrder( ), Boolean.FALSE ) ? ordering.reverse() : ordering );
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
              domains.lookupByName( accountFullName, request.getDomain(), Registered, Functions.identity() );
          if ( workflowTypes.countByDomain( accountFullName, domain.getDisplayName() ) >=
              SimpleWorkflowProperties.getWorkflowTypesPerDomain() ) {
            throw upClient( "LimitExceededFault", "Request would exceed limit for type: workflow-type" );
          }
          final WorkflowType workflowType = WorkflowType.create(
              userFullName,
              request.getName(),
              request.getVersion(),
              domain,
              request.getDescription(),
              request.getDefaultTaskList() == null ? null : request.getDefaultTaskList().getName(),
              request.getDefaultChildPolicy(),
              parsePeriod( request.getDefaultExecutionStartToCloseTimeout(), -1 ),
              parsePeriod( request.getDefaultTaskStartToCloseTimeout(), -1 )
          );
          return workflowTypes.save( workflowType );
        } catch ( SwfMetadataNotFoundException e ) {
          throw upClient( "UnknownResourceFault", "Unknown domain: " + request.getDomain( ) );
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
          workflowType -> {
            if ( accessible.apply( workflowType ) ) {
              if ( workflowType.getState( ) == WorkflowType.Status.Deprecated ) {
                throw upClient(
                    "TypeDeprecatedFault",
                    "Workflow type already deprecated: " + request.getWorkflowType().getName() );
              }
              workflowType.setState( WorkflowType.Status.Deprecated );
              workflowType.setDeprecationTimestamp( new Date( ) );
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
      workflowTypeInfos.getTypeInfos( ).addAll( workflowTypes.list(
          accountFullName,
          requestedAndAccessible,
          TypeMappers.lookup( WorkflowType.class, WorkflowTypeInfo.class ) ) );
      final Ordering<WorkflowTypeInfo> ordering =
          Ordering.natural( ).onResultOf( WorkflowTypes.WorkflowTypeInfoStringFunctions.NAME );
      Collections.sort(
          workflowTypeInfos.getTypeInfos( ),
          MoreObjects.firstNonNull( request.getReverseOrder( ), Boolean.FALSE ) ? ordering.reverse() : ordering );
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
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    final WorkflowExecutionDetail workflowExecutionDetail;
    try {
      workflowExecutionDetail = workflowExecutions.lookupByExample(
          WorkflowExecution.exampleWithName( accountFullName, request.getExecution( ).getRunId( ) ),
          accountFullName,
          request.getExecution( ).getRunId( ),
          accessible,
          workflowExecution -> {
            final WorkflowExecutionDetail detail =
                TypeMappers.transform( workflowExecution, WorkflowExecutionDetail.class );
            final Iterable<WorkflowHistoryEvent> events = workflowExecution.getWorkflowHistory();
            final int openActivities =
                CollectionUtils.reduce( events, 0, CollectionUtils.count(
                    CollectionUtils.propertyPredicate( "ActivityTaskScheduled", EVENT_TYPE ) ) ) -
                CollectionUtils.reduce( events, 0, CollectionUtils.count(
                    CollectionUtils.propertyPredicate( WorkflowExecutions.ACTIVITY_CLOSE_EVENT_TYPES, EVENT_TYPE ) ) );
            final int openTimers =
                CollectionUtils.reduce( events, 0, CollectionUtils.count(
                    CollectionUtils.propertyPredicate( "TimerStarted", EVENT_TYPE ) ) ) -
                    CollectionUtils.reduce( events, 0, CollectionUtils.count(
                        CollectionUtils.propertyPredicate( WorkflowExecutions.TIMER_CLOSE_EVENT_TYPES, EVENT_TYPE ) ) );
            detail.withOpenCounts( new WorkflowExecutionOpenCounts( )
              .withOpenActivityTasks( openActivities )
              .withOpenChildWorkflowExecutions( 0 )
              .withOpenDecisionTasks( workflowExecution.getDecisionStatus( ) != Idle ? 1 : 0 )
              .withOpenTimers( openTimers )
              .withOpenLambdaFunctions( 0 ) );
            return detail;
          }
      );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          "Unknown execution, runId = " + request.getExecution().getRunId( ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }

    return request.reply( workflowExecutionDetail );
  }

  public WorkflowExecutionCount countClosedWorkflowExecutions( final CountClosedWorkflowExecutionsRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super Domain> accessible =
        SimpleWorkflowMetadatas.filteringFor( Domain.class ).byPrivileges( ).buildPredicate( );

    final WorkflowExecutionCount workflowExecutionCount;
    try {
      workflowExecutionCount = domains.lookupByExample(
          Domain.exampleWithName( accountFullName, request.getDomain( ) ),
          accountFullName,
          request.getDomain( ),
          accessible,
          domain -> {
            final Conjunction filter = Restrictions.conjunction( );
            final Map<String,String> aliases = Maps.newHashMap( );
            buildFilters( request, filter, aliases );
            //noinspection deprecation
            return new WorkflowExecutionCount( )
                .withCount( (int) Entities.count(
                    WorkflowExecution.exampleForClosedWorkflow( accountFullName, request.getDomain( ), null ),
                    filter,
                    aliases
                 ) )
                .withTruncated( false );
          }
      );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          "Unknown domain, name = " + request.getDomain( ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }

    return request.reply( workflowExecutionCount );
  }

  public WorkflowExecutionCount countOpenWorkflowExecutions( final CountOpenWorkflowExecutionsRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super Domain> accessible =
        SimpleWorkflowMetadatas.filteringFor( Domain.class ).byPrivileges( ).buildPredicate( );

    final WorkflowExecutionCount workflowExecutionCount;
    try {
      workflowExecutionCount = domains.lookupByExample(
          Domain.exampleWithName( accountFullName, request.getDomain( ) ),
          accountFullName,
          request.getDomain( ),
          accessible,
          domain -> {
            final Conjunction filter = Restrictions.conjunction( );
            final Map<String,String> aliases = Maps.newHashMap( );
            buildFilters( request, filter, aliases );
            //noinspection deprecation
            return new WorkflowExecutionCount( )
                .withCount( (int) Entities.count(
                    WorkflowExecution.exampleForOpenWorkflow( accountFullName, request.getDomain(), null ),
                    filter,
                    aliases
                ) )
                .withTruncated( false );
          }
      );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          "Unknown domain, name = " + request.getDomain( ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }

    return request.reply( workflowExecutionCount );
  }

  public PendingTaskCount countPendingActivityTasks( final CountPendingActivityTasksRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super Domain> accessible =
        SimpleWorkflowMetadatas.filteringFor( Domain.class ).byPrivileges( ).buildPredicate( );

    final PendingTaskCount pendingTaskCount;
    try {
      //noinspection deprecation
      pendingTaskCount = domains.lookupByExample(
          Domain.exampleWithName( accountFullName, request.getDomain( ) ),
          accountFullName,
          request.getDomain(),
          accessible,
          domain -> new PendingTaskCount( )
              .withCount( (int) Entities.count( ActivityTask.examplePending(
                  accountFullName,
                  request.getDomain( ),
                  request.getTaskList( ).getName( ) ) ) )
              .withTruncated( false )
      );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          "Unknown domain, name = " + request.getDomain( ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }

    return request.reply( pendingTaskCount );
  }

  public PendingTaskCount countPendingDecisionTasks( final CountPendingDecisionTasksRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super Domain> accessible =
        SimpleWorkflowMetadatas.filteringFor( Domain.class ).byPrivileges( ).buildPredicate( );

    final PendingTaskCount pendingTaskCount;
    try {
      //noinspection deprecation
      pendingTaskCount = domains.lookupByExample(
          Domain.exampleWithName( accountFullName, request.getDomain( ) ),
          accountFullName,
          request.getDomain( ),
          accessible,
          domain -> new PendingTaskCount( )
              .withCount( (int) Entities.count( WorkflowExecution.exampleWithPendingDecision(
                  accountFullName,
                  request.getDomain(),
                  request.getTaskList().getName() ) ) )
              .withTruncated( false )
      );
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          "Unknown domain, name = " + request.getDomain( ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }

    return request.reply( pendingTaskCount );
  }

  public WorkflowExecutionInfos listClosedWorkflowExecutions( final ListClosedWorkflowExecutionsRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    final WorkflowExecutionInfos workflowExecutionInfos = new WorkflowExecutionInfos( );
    try {
      final Conjunction filter = Restrictions.conjunction( );
      final Map<String,String> aliases = Maps.newHashMap( );
      buildFilters( request, filter, aliases );
      workflowExecutionInfos.getExecutionInfos( ).addAll( workflowExecutions.listByExample(
          WorkflowExecution.exampleForClosedWorkflow( accountFullName, request.getDomain( ), null ),
          accessible,
          filter,
          aliases,
          TypeMappers.lookup( WorkflowExecution.class, WorkflowExecutionInfo.class )
      ) );
      final Ordering<WorkflowExecutionInfo> ordering =
          Ordering.natural( ).onResultOf( WorkflowExecutions.WorkflowExecutionInfoDateFunctions.START_TIMESTAMP );
      Collections.sort(
          workflowExecutionInfos.getExecutionInfos( ),
          MoreObjects.firstNonNull( request.getReverseOrder( ), Boolean.FALSE ) ? ordering.reverse() : ordering );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return request.reply( workflowExecutionInfos );
  }

  public WorkflowExecutionInfos listOpenWorkflowExecutions( final ListOpenWorkflowExecutionsRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    final WorkflowExecutionInfos workflowExecutionInfos = new WorkflowExecutionInfos( );
    try {
      final Conjunction filter = Restrictions.conjunction( );
      final Map<String,String> aliases = Maps.newHashMap( );
      buildFilters( request, filter, aliases );
      workflowExecutionInfos.getExecutionInfos( ).addAll( workflowExecutions.listByExample(
          WorkflowExecution.exampleForOpenWorkflow( accountFullName, request.getDomain(), null ),
          accessible,
          filter,
          aliases,
          TypeMappers.lookup( WorkflowExecution.class, WorkflowExecutionInfo.class )
      ) );
      final Ordering<WorkflowExecutionInfo> ordering =
          Ordering.natural( ).onResultOf( WorkflowExecutions.WorkflowExecutionInfoDateFunctions.START_TIMESTAMP );
      Collections.sort(
          workflowExecutionInfos.getExecutionInfos( ),
          MoreObjects.firstNonNull( request.getReverseOrder( ), Boolean.FALSE ) ? ordering.reverse() : ordering );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return request.reply( workflowExecutionInfos );
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

          final Domain domain;
          try {
              domain = domains.lookupByName( accountFullName, request.getDomain( ), Registered, Functions.identity( ) );
          } catch ( SwfMetadataNotFoundException e ) {
            throw upClient( "UnknownResourceFault", "Unknown domain: " + request.getDomain() );
          }
          if ( workflowExecutions.countOpenByDomain( accountFullName, domain.getDisplayName( ) ) >=
              SimpleWorkflowProperties.getOpenWorkflowExecutionsPerDomain() ) {
            throw upClient( "LimitExceededFault", "Request would exceed limit for open workflow executions" );
          }
          final WorkflowType workflowType;
          try {
            workflowType = workflowTypes.lookupByExample(
                WorkflowType.exampleWithUniqueName(
                    accountFullName,
                    request.getDomain(),
                    request.getWorkflowType().getName(),
                    request.getWorkflowType().getVersion() ),
                accountFullName,
                request.getWorkflowType().getName(),
                Predicates.and( accessible, WorkflowType.Status.Registered ),
                Functions.<WorkflowType>identity() );
          } catch ( SwfMetadataNotFoundException e ) {
            throw upClient( "UnknownResourceFault", "Unknown workflow type: " + request.getWorkflowType().getName() );
          }
          if ( request.getChildPolicy( ) == null && workflowType.getDefaultChildPolicy( ) == null ) {
            throw upClient( "DefaultUndefinedFault", "Default child policy undefined" );
          }
          if ( request.getTaskList( ) == null && workflowType.getDefaultTaskList( ) == null ) {
            throw upClient( "DefaultUndefinedFault", "Default task list undefined" );
          }
          final String childPolicy = MoreObjects.firstNonNull(
              request.getChildPolicy( ),
              workflowType.getDefaultChildPolicy( ) );
          final String taskList = request.getTaskList( ) == null ?
              workflowType.getDefaultTaskList( ):
              request.getTaskList( ).getName( );
          final Integer executionStartToCloseTimeout = requireDefault(
              parsePeriod( request.getExecutionStartToCloseTimeout(), -1 ),
              workflowType.getDefaultExecutionStartToCloseTimeout(), "ExecutionStartToCloseTimeout" );
          final Integer taskStartToCloseTimeout = requireDefault(
              parsePeriod( request.getTaskStartToCloseTimeout(), -1 ),
              workflowType.getDefaultTaskStartToCloseTimeout(), "TaskStartToCloseTimeout" );
          final String taskStartToCloseTimeoutStr = taskStartToCloseTimeout < 0
              ? "NONE" :
              String.valueOf( taskStartToCloseTimeout );
            final WorkflowExecution workflowExecution = WorkflowExecution.create(
              userFullName,
              UUID.randomUUID( ).toString( ),
              domain,
              workflowType,
              request.getWorkflowId( ),
              childPolicy,
              taskList,
              executionStartToCloseTimeout,
              taskStartToCloseTimeout < 0 ? null : taskStartToCloseTimeout,
              request.getTagList( ),
              Lists.newArrayList(
                  new WorkflowExecutionStartedEventAttributes( )
                      .withChildPolicy( childPolicy )
                      .withExecutionStartToCloseTimeout( String.valueOf( executionStartToCloseTimeout ) )
                      .withInput( request.getInput( ) )
                      .withParentInitiatedEventId( 0L )
                      .withTaskList( new TaskList( ).withName( taskList ) )
                      .withTagList( request.getTagList( ) )
                      .withTaskStartToCloseTimeout( taskStartToCloseTimeoutStr )
                      .withWorkflowType( request.getWorkflowType( ) ),
                  new DecisionTaskScheduledEventAttributes( )
                      .withStartToCloseTimeout( taskStartToCloseTimeoutStr )
                      .withTaskList( new TaskList( ).withName( taskList ) )
              )
          );
            return workflowExecutions.save( workflowExecution );
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    }, WorkflowExecution.class, request.getWorkflowId( ) );

    notifyTaskList( accountFullName, workflowExecution.getDomainName( ), "decision", workflowExecution.getTaskList( ) );

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

    final String domain = request.getDomain( );
    final String taskList = request.getTaskList( ).getName( );
    final Callable<com.eucalyptus.simpleworkflow.common.model.ActivityTask> taskCallable =
        () -> {
          com.eucalyptus.simpleworkflow.common.model.ActivityTask activityTask = null;
          final List<ActivityTask> pending = activityTasks.listByExample(
              ActivityTask.examplePending( accountFullName, domain, taskList ),
              accessible,
              Functions.identity( ) );
          Collections.sort( pending, Ordering.natural( ).onResultOf( AbstractPersistentSupport.creation( ) ) );
          for ( final List<ActivityTask> pendingSegment : shufflePartitions( pending ) ) {
            for ( final ActivityTask pendingTask : pendingSegment ) {
              if ( activityTask != null ) break;
              try ( final WorkflowLock lock = WorkflowLock.tryLock(
                  accountFullName,
                  pendingTask.getDomainUuid( ),
                  pendingTask.getWorkflowRunId() ) ) {
                if ( !lock.isHeldByCurrentThread( ) ) {
                  continue;
                }
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
                          Entities.lock(workflowExecution);
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

              } catch ( SwfMetadataException e ) {
                logger.debug( "Activity task for domain " + domain + ", list " + taskList + " not found" );
              } catch ( Exception e ) {
                if ( PersistenceExceptions.isStaleUpdate( e ) ) {
                  logger.info( "Activity task for domain " + domain + ", list " + taskList + " already taken"  );
                } else if (  PersistenceExceptions.isLockError( e ) ) {
                  logger.info( "Activity task for domain " + domain + ", list " + taskList + " locking error" );
                } else {
                  logger.error( "Error taking activity task for domain " + domain + ", list " + taskList, e );
                }
              }
            }
          }
          return activityTask;
        };

    try {
      return handleTaskPolling( accountFullName, domain, "activity", taskList, request.getCorrelationId( ), new com.eucalyptus.simpleworkflow.common.model.ActivityTask( ), taskCallable );
    } catch ( Exception e ) {
      throw handleException( e );
    }
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

      activityTasks.withRetries( ).updateByExample(
          ActivityTask.exampleWithUniqueName( accountFullName, token.getRunId(), token.getScheduledEventId() ),
          accountFullName,
          token.getRunId( ) + "/" + token.getScheduledEventId( ),
          activityTask -> {
            if ( accessible.apply( activityTask ) ) {
              activityTask.setHeartbeatDetails( request.getDetails( ) );
              activityTask.updateTimeStamps( );
              status.setCancelRequested( activityTask.getCancelRequestedEventId( ) != null );
            }
            return activityTask;
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

      final Pair<String,String> domainTaskListPair;
      try ( final WorkflowLock lock = WorkflowLock.lock( accountFullName, token.getDomainUuid( ), token.getRunId( ) ) ) {
        domainTaskListPair = activityTasks.withRetries().updateByExample(
            ActivityTask.exampleWithUniqueName( accountFullName, token.getRunId(), token.getScheduledEventId() ),
            accountFullName,
            token.getRunId() + "/" + token.getScheduledEventId(),
            activityTask -> {
              if ( accessible.apply( activityTask ) ) {
                final WorkflowExecution workflowExecution = activityTask.getWorkflowExecution();
                workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                    workflowExecution,
                    new ActivityTaskCanceledEventAttributes()
                        .withDetails( request.getDetails() )
                        .withLatestCancelRequestedEventId( activityTask.getCancelRequestedEventId() )
                        .withScheduledEventId( activityTask.getScheduledEventId() )
                        .withStartedEventId( activityTask.getStartedEventId() )
                ) );
                if ( workflowExecution.getDecisionStatus() != Pending ) {
                  workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                      workflowExecution,
                      new DecisionTaskScheduledEventAttributes()
                          .withTaskList( new TaskList().withName( workflowExecution.getTaskList() ) )
                          .withStartToCloseTimeout( String.valueOf( workflowExecution.getTaskStartToCloseTimeout() ) )
                  ) );
                  if ( workflowExecution.getDecisionStatus() == Idle ) {
                    workflowExecution.setDecisionStatus( Pending );
                    workflowExecution.setDecisionTimestamp( new Date() );
                  }
                }
                Entities.delete( activityTask );
                return workflowExecution.getDecisionStatus() == Pending ?
                    Pair.pair( workflowExecution.getDomainName(), workflowExecution.getTaskList() ) :
                    null;
              }
              return null;
            } );
      }

      if ( domainTaskListPair != null ) {
        notifyTaskList( accountFullName, domainTaskListPair.getLeft(), "decision", domainTaskListPair.getRight() );
      }
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          "Unknown activity task, token = " + request.getTaskToken( ) );
    } catch( Exception e ) {
      throw handleException( e );
    }

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
          Functions.identity( ) );

      final WorkflowExecution workflowExecution;
      try ( final WorkflowLock lock = WorkflowLock.lock( accountFullName, domain, token.getRunId( ) ) ) {
        workflowExecution = workflowExecutions.withRetries().updateByExample(
            WorkflowExecution.exampleWithUniqueName( accountFullName, domain.getDisplayName(), token.getRunId() ),
            accountFullName,
            token.getRunId(),
            new Function<WorkflowExecution, WorkflowExecution>() {
              @Nullable
              @Override
              public WorkflowExecution apply( final WorkflowExecution workflowExecution ) {
                if ( accessible.apply( workflowExecution ) ) {
                  Entities.lock(workflowExecution);
                  try {
                    activityTasks.deleteByExample( ActivityTask.exampleWithUniqueName(
                        accountFullName,
                        token.getRunId(),
                        token.getScheduledEventId() ) );
                  } catch ( SwfMetadataException e ) {
                    throw up( e );
                  }

                  // TODO:STEVE: verify token valid (no reuse, etc)
                  workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                      workflowExecution,
                      new ActivityTaskCompletedEventAttributes()
                          .withResult( request.getResult() )
                          .withScheduledEventId( token.getScheduledEventId() )
                          .withStartedEventId( token.getStartedEventId() )
                  ) );
                  if ( workflowExecution.getDecisionStatus() != Pending ) {
                    workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                        workflowExecution,
                        new DecisionTaskScheduledEventAttributes()
                            .withTaskList( new TaskList().withName( workflowExecution.getTaskList() ) )
                            .withStartToCloseTimeout( String.valueOf( workflowExecution.getTaskStartToCloseTimeout() ) )
                    ) );
                    if ( workflowExecution.getDecisionStatus() == Idle ) {
                      workflowExecution.setDecisionStatus( Pending );
                      workflowExecution.setDecisionTimestamp( new Date() );
                    }
                  }
                }
                return workflowExecution;
              }
            } );
      }

      if ( workflowExecution.getDecisionStatus() == Pending ) {
        notifyTaskList( accountFullName, workflowExecution.getDomainName(), "decision", workflowExecution.getTaskList() );
      }
    } catch( Exception e ) {
      throw handleException( e );
    }

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
          Functions.identity( ) );

      final WorkflowExecution workflowExecution;
      try ( final WorkflowLock lock = WorkflowLock.lock( accountFullName, domain, token.getRunId( ) ) ) {
        workflowExecution = workflowExecutions.withRetries().updateByExample(
            WorkflowExecution.exampleWithUniqueName( accountFullName, domain.getDisplayName(), token.getRunId() ),
            accountFullName,
            token.getRunId(),
            new Function<WorkflowExecution, WorkflowExecution>() {
              @Nullable
              @Override
              public WorkflowExecution apply( final WorkflowExecution workflowExecution ) {
                if ( accessible.apply( workflowExecution ) ) {
                  Entities.lock(workflowExecution);
                  try {
                    activityTasks.deleteByExample( ActivityTask.exampleWithUniqueName(
                        accountFullName,
                        token.getRunId(),
                        token.getScheduledEventId() ) );
                  } catch ( SwfMetadataException e ) {
                    throw up( e );
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
                  if ( workflowExecution.getDecisionStatus() != Pending ) {
                    workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                        workflowExecution,
                        new DecisionTaskScheduledEventAttributes()
                            .withTaskList( new TaskList().withName( workflowExecution.getTaskList() ) )
                            .withStartToCloseTimeout( String.valueOf( workflowExecution.getTaskStartToCloseTimeout() ) )
                    ) );
                    if ( workflowExecution.getDecisionStatus() == Idle ) {
                      workflowExecution.setDecisionStatus( Pending );
                      workflowExecution.setDecisionTimestamp( new Date() );
                    }
                  }
                }
                return workflowExecution;
              }
            } );
      }

      if ( workflowExecution.getDecisionStatus( ) == Pending ) {
        notifyTaskList( accountFullName, workflowExecution.getDomainName(), "decision", workflowExecution.getTaskList() );
      }
    } catch( Exception e ) {
      throw handleException( e );
    }

    return request.reply( new SimpleWorkflowMessage() );
  }

  public DecisionTask pollForDecisionTask( final PollForDecisionTaskRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    final String domain = request.getDomain( );
    final String taskList = request.getTaskList( ).getName( );
    final Callable<DecisionTask> taskCallable = () -> {
      final List<WorkflowExecution> pending = workflowExecutions.listByExample(
          WorkflowExecution.exampleWithPendingDecision( accountFullName, domain, taskList ),
          accessible,
          Functions.identity( ) );
      Collections.sort( pending, Ordering.natural( ).onResultOf( AbstractPersistentSupport.creation( ) ) );
      DecisionTask decisionTask = null;
      for ( final List<WorkflowExecution> pendingSegment : shufflePartitions( pending ) ) {
        for ( final WorkflowExecution execution : pendingSegment ) {
          if ( decisionTask != null ) break;
          try ( final WorkflowLock lock = WorkflowLock.tryLock(
              accountFullName,
              execution.getDomainUuid( ),
              execution.getDisplayName( ) ) ) {
            if ( !lock.isHeldByCurrentThread( ) ) {
              continue;
            }
            decisionTask = workflowExecutions.updateByExample(
                WorkflowExecution.exampleWithUniqueName( accountFullName, execution.getDomainName( ), execution.getDisplayName( ) ),
                accountFullName,
                execution.getDisplayName(),
                new Function<WorkflowExecution,DecisionTask>( ) {
                  @Nullable
                  @Override
                  public DecisionTask apply( final WorkflowExecution workflowExecution ) {
                    if ( workflowExecution.getDecisionStatus( ) == Pending ) {
                      Entities.lock(workflowExecution);
                      final List<WorkflowHistoryEvent> events = workflowExecution.getWorkflowHistory();
                      final List<WorkflowHistoryEvent> reverseEvents = Lists.reverse( events );
                      final WorkflowHistoryEvent scheduled = Iterables.find(
                          reverseEvents,
                          CollectionUtils.propertyPredicate( "DecisionTaskScheduled", EVENT_TYPE ) );
                      final Optional<WorkflowHistoryEvent> previousStarted = Iterables.tryFind(
                          reverseEvents,
                          CollectionUtils.propertyPredicate( "DecisionTaskStarted", EVENT_TYPE ) );
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
                              MoreObjects.firstNonNull( request.isReverseOrder( ), Boolean.FALSE ) ? reverseEvents : events,
                              TypeMappers.lookup( WorkflowHistoryEvent.class, HistoryEvent.class )
                          ) );
                    }
                    return null;
                  }
                } );
          } catch ( Exception e ) {
            final StaleObjectStateException stale = Exceptions.findCause( e, StaleObjectStateException.class );
            if ( stale != null ) try {
              Entities.evictCache( Class.forName( stale.getEntityName( ) ) );
            } catch ( ClassNotFoundException ce ) { /* eviction failure */ }
            if ( PersistenceExceptions.isStaleUpdate( e ) ) {
              logger.debug( "Decision task for workflow " + execution.getDisplayName() + " already taken" );
            } else if (  PersistenceExceptions.isLockError( e ) ) {
              logger.info( "Decision task for workflow " + execution.getDisplayName() + " locking error" );
            } else {
              logger.error( "Error taking decision task for workflow " + execution.getDisplayName( ), e );
            }
          }
        }
      }
      return decisionTask;
    };

    try {
      return handleTaskPolling( accountFullName, domain, "decision", taskList, request.getCorrelationId(), new DecisionTask(), taskCallable );
    } catch ( Exception e ) {
      throw handleException( e );
    }
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
          Functions.identity( ) );

      final Set<Pair<String,String>> notificationTypeListPairs = Sets.newHashSet( );
      try ( final WorkflowLock lock = WorkflowLock.lock( accountFullName, domain, token.getRunId() ) ) {
        workflowExecutions.withRetries( ).updateByExample(
          WorkflowExecution.exampleWithUniqueName( accountFullName, domain.getDisplayName( ), token.getRunId( ) ),
          accountFullName,
          token.getRunId( ),
          new Function<WorkflowExecution, WorkflowExecution>() {
            @Nullable
            @Override
            public WorkflowExecution apply( final WorkflowExecution workflowExecution ) {
              if ( accessible.apply( workflowExecution ) ) {
                Entities.lock(workflowExecution);

                // clear pending notifications in case of retries
                notificationTypeListPairs.clear( );

                // verify token is valid
                final List<WorkflowHistoryEvent> events = workflowExecution.getWorkflowHistory();
                final List<WorkflowHistoryEvent> reverseEvents = Lists.reverse( events );
                final WorkflowHistoryEvent started = Iterables.find(
                    reverseEvents,
                    CollectionUtils.propertyPredicate( "DecisionTaskStarted", EVENT_TYPE ) );
                if ( !started.getEventId( ).equals( token.getStartedEventId( ) ) ) {
                  throw upClient( "ValidationError", "Bad token" );
                }
                final WorkflowHistoryEvent scheduled = Iterables.find(
                    reverseEvents,
                    CollectionUtils.propertyPredicate( "DecisionTaskScheduled", EVENT_TYPE ) );
                if ( scheduled.getEventId( ) < started.getEventId() ) {
                  workflowExecution.setDecisionStatus( Idle );
                  workflowExecution.setDecisionTimestamp( new Date( ) );
                } else {
                  workflowExecution.setDecisionStatus( Pending );
                  workflowExecution.setDecisionTimestamp( new Date( ) );
                  notificationTypeListPairs.add( Pair.pair( "decision", workflowExecution.getTaskList( ) ) );
                }

                // setup activity count supplier
                int activityTaskScheduledCount = 0;
                final Supplier<Long> activityTaskCounter = Suppliers.memoize( () -> {
                  try {
                    return activityTasks.countByWorkflowExecution(
                        accountFullName,
                        domain.getDisplayName( ),
                        workflowExecution.getDisplayName( ) );
                  } catch ( SwfMetadataException e ) {
                    throw up( e );
                  }
                } );

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
                      try {
                        final List<Timer> timerList = timers.listByExample(
                            Timer.exampleWithTimerId(
                                accountFullName,
                                workflowExecution.getDomainName( ),
                                workflowExecution.getDisplayName( ),
                                cancelTimer.getTimerId( ) ),
                            Predicates.alwaysTrue( ),
                            Functions.identity( )
                        );

                        if ( !timerList.isEmpty( ) ) {
                          final Timer timer = Iterables.getOnlyElement( timerList );
                          timers.deleteByExample( timer );
                          workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                              workflowExecution,
                              new TimerCanceledEventAttributes( )
                                  .withDecisionTaskCompletedEventId( completedId )
                                  .withStartedEventId( timer.getStartedEventId( ) )
                                  .withTimerId( cancelTimer.getTimerId( ) )
                          ) );
                        } else {
                          workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                              workflowExecution,
                              new CancelTimerFailedEventAttributes()
                                  .withCause( CancelTimerFailedCause.TIMER_ID_UNKNOWN )
                                  .withDecisionTaskCompletedEventId( completedId )
                                  .withTimerId( cancelTimer.getTimerId( ) )
                          ) );
                          scheduleDecisionTask = true;
                        }
                      } catch ( SwfMetadataException e ) {
                        throw up( e );
                      }
                      break;
                    case "CancelWorkflowExecution":
                      final CancelWorkflowExecutionDecisionAttributes cancelWorkflowExecution =
                          decision.getCancelWorkflowExecutionDecisionAttributes();
                      workflowExecution.closeWorkflow(
                          WorkflowExecution.CloseStatus.Canceled,
                          WorkflowHistoryEvent.create(
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
                      workflowExecution.closeWorkflow(
                          WorkflowExecution.CloseStatus.Completed,
                          WorkflowHistoryEvent.create(
                              workflowExecution,
                              new WorkflowExecutionCompletedEventAttributes( )
                                .withDecisionTaskCompletedEventId( completedId )
                                .withResult( completed.getResult( ) )
                      ) );
                      break;
                    case "ContinueAsNewWorkflowExecution":
                      final ContinueAsNewWorkflowExecutionDecisionAttributes continueAsNew =
                          decision.getContinueAsNewWorkflowExecutionDecisionAttributes( );

                      ContinueAsNewWorkflowExecutionFailedCause failedCause = null;
                      if ( Pending == workflowExecution.getDecisionStatus( ) ) {
                        failedCause = ContinueAsNewWorkflowExecutionFailedCause.UNHANDLED_DECISION;
                      }

                      WorkflowType workflowType = null;
                      if ( failedCause == null ) try {
                        workflowType = workflowTypes.lookupByExample(
                            WorkflowType.exampleWithUniqueName(
                                accountFullName,
                                workflowExecution.getDomainName( ),
                                workflowExecution.getWorkflowType( ).getName( ),
                                MoreObjects.firstNonNull(
                                    continueAsNew.getWorkflowTypeVersion( ),
                                    workflowExecution.getWorkflowType( ).getWorkflowVersion( ) ) ),
                            accountFullName,
                            workflowExecution.getWorkflowType( ).getName( ),
                            Predicates.alwaysTrue( ),
                            Functions.identity( ) );

                        if ( !WorkflowType.Status.Registered.apply( workflowType ) ) {
                          failedCause = ContinueAsNewWorkflowExecutionFailedCause.WORKFLOW_TYPE_DEPRECATED;
                        }
                      } catch ( SwfMetadataNotFoundException e ) {
                        failedCause = ContinueAsNewWorkflowExecutionFailedCause.WORKFLOW_TYPE_DOES_NOT_EXIST;
                      } catch ( SwfMetadataException e ) {
                        throw up( e );
                      }

                      if ( failedCause == null ) {
                        final Integer requestedExecutionStartToCloseTimeout = parsePeriod( continueAsNew.getExecutionStartToCloseTimeout( ), -1 );
                        final Integer requestedTaskStartToCloseTimeout = parsePeriod( continueAsNew.getTaskStartToCloseTimeout( ), -1 );
                        if ( continueAsNew.getChildPolicy( ) == null && workflowType.getDefaultChildPolicy( ) == null ) {
                          failedCause = ContinueAsNewWorkflowExecutionFailedCause.DEFAULT_CHILD_POLICY_UNDEFINED;
                        } else if ( requestedExecutionStartToCloseTimeout == null && workflowType.getDefaultExecutionStartToCloseTimeout( ) == null ) {
                          failedCause = ContinueAsNewWorkflowExecutionFailedCause.DEFAULT_EXECUTION_START_TO_CLOSE_TIMEOUT_UNDEFINED;
                        } else if ( continueAsNew.getTaskList( ) == null && workflowType.getDefaultTaskList( ) == null ) {
                          failedCause = ContinueAsNewWorkflowExecutionFailedCause.DEFAULT_TASK_LIST_UNDEFINED;
                        } else if ( requestedTaskStartToCloseTimeout == null && workflowType.getDefaultTaskStartToCloseTimeout( ) == null ) {
                          failedCause = ContinueAsNewWorkflowExecutionFailedCause.DEFAULT_TASK_START_TO_CLOSE_TIMEOUT_UNDEFINED;
                        } else {
                          final String childPolicy = MoreObjects.firstNonNull(
                              continueAsNew.getChildPolicy( ),
                              workflowType.getDefaultChildPolicy( ) );
                          final String taskList = continueAsNew.getTaskList( ) == null ?
                              workflowType.getDefaultTaskList( ):
                              continueAsNew.getTaskList( ).getName( );
                          final Integer executionStartToCloseTimeout = MoreObjects.firstNonNull(
                              requestedExecutionStartToCloseTimeout,
                              workflowType.getDefaultExecutionStartToCloseTimeout( ) );
                          final Integer taskStartToCloseTimeout = MoreObjects.firstNonNull(
                              requestedTaskStartToCloseTimeout,
                              workflowType.getDefaultTaskStartToCloseTimeout( ) );
                          final String taskStartToCloseTimeoutStr = taskStartToCloseTimeout < 0
                              ? "NONE" :
                              String.valueOf( taskStartToCloseTimeout );
                          final WorkflowExecution workflowExecutionContinued = WorkflowExecution.create(
                              userFullName,
                              UUID.randomUUID( ).toString( ),
                              domain,
                              workflowType,
                              workflowExecution.getWorkflowId( ),
                              childPolicy,
                              taskList,
                              executionStartToCloseTimeout,
                              taskStartToCloseTimeout < 0 ? null : taskStartToCloseTimeout,
                              continueAsNew.getTagList( ),
                              Lists.newArrayList(
                                  new WorkflowExecutionStartedEventAttributes( )
                                      .withChildPolicy( childPolicy )
                                      .withContinuedExecutionRunId( workflowExecution.getDisplayName( ) )
                                      .withExecutionStartToCloseTimeout( String.valueOf( executionStartToCloseTimeout ) )
                                      .withInput( continueAsNew.getInput( ) )
                                      .withParentInitiatedEventId( 0L )
                                      .withTaskList( new TaskList( ).withName( taskList ) )
                                      .withTagList( continueAsNew.getTagList( ) )
                                      .withTaskStartToCloseTimeout( taskStartToCloseTimeoutStr )
                                      .withWorkflowType( new com.eucalyptus.simpleworkflow.common.model.WorkflowType( )
                                        .withName( workflowType.getDisplayName( ) )
                                        .withVersion( workflowType.getWorkflowVersion( ) )
                                      ),
                                  new DecisionTaskScheduledEventAttributes( )
                                      .withStartToCloseTimeout( taskStartToCloseTimeoutStr )
                                      .withTaskList( new TaskList( ).withName( taskList ) )
                              )
                          );
                          try {
                            workflowExecutions.save( workflowExecutionContinued );
                          } catch ( SwfMetadataException e ) {
                            throw up( e );
                          }

                          workflowExecution.closeWorkflow(
                              WorkflowExecution.CloseStatus.Continued_As_New,
                              WorkflowHistoryEvent.create(
                                  workflowExecution,
                                  new WorkflowExecutionCompletedEventAttributes( )
                                      .withDecisionTaskCompletedEventId( completedId )
                              ) );

                          deleteActivities( activityTasks, accountFullName, workflowExecution );

                          notificationTypeListPairs.add( Pair.pair( "decision", taskList ) );
                        }
                      }

                      if ( failedCause != null ) {
                        workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                            workflowExecution,
                            new ContinueAsNewWorkflowExecutionFailedEventAttributes( )
                                .withCause( ContinueAsNewWorkflowExecutionFailedCause.OPERATION_NOT_PERMITTED )
                                .withDecisionTaskCompletedEventId( completedId )
                        ) );
                        scheduleDecisionTask = true;
                      }
                      break;
                    case "FailWorkflowExecution":
                      final FailWorkflowExecutionDecisionAttributes failed =
                          decision.getFailWorkflowExecutionDecisionAttributes();
                      workflowExecution.closeWorkflow(
                          WorkflowExecution.CloseStatus.Failed,
                          WorkflowHistoryEvent.create(
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
                            (Function<ActivityTask, Void>) activityTask -> {
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
                        throw up( e );
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
                        final ActivityType activityType;
                        try {
                          activityType = activityTypes.lookupByExample(
                              ActivityType.exampleWithUniqueName(
                                  accountFullName,
                                  domain.getDisplayName(),
                                  scheduleActivity.getActivityType().getName(),
                                  scheduleActivity.getActivityType().getVersion() ),
                              accountFullName,
                              scheduleActivity.getActivityType().getName(),
                              Predicates.alwaysTrue(),
                              Functions.identity() );
                        } catch ( final SwfMetadataNotFoundException e ) {
                          throw new ScheduleActivityTaskException( ACTIVITY_TYPE_DOES_NOT_EXIST );
                        }

                        if ( ActivityType.Status.Deprecated.apply( activityType ) ) {
                          throw new ScheduleActivityTaskException( ACTIVITY_TYPE_DEPRECATED );
                        }

                        final String list = scheduleActivity.getTaskList( ) == null ?
                            activityType.getDefaultTaskList( ) :
                            scheduleActivity.getTaskList( ).getName( );
                        if ( list == null ) {
                          throw new ScheduleActivityTaskException( DEFAULT_TASK_LIST_UNDEFINED );
                        }

                        if ( activityTaskCounter.get( ) + activityTaskScheduledCount >=
                            SimpleWorkflowProperties.getOpenActivityTasksPerWorkflowExecution() ) {
                          throw new ScheduleActivityTaskException( OPEN_ACTIVITIES_LIMIT_EXCEEDED );
                        }

                        activityTasks.save( com.eucalyptus.simpleworkflow.ActivityTask.create(
                            userFullName,
                            workflowExecution,
                            domain.getDisplayName(),
                            domain.getNaturalId( ),
                            scheduleActivity.getActivityId(),
                            scheduleActivity.getActivityType().getName(),
                            scheduleActivity.getActivityType().getVersion(),
                            scheduleActivity.getInput(),
                            scheduledId,
                            list,
                            parseActivityPeriod(
                                scheduleActivity.getScheduleToCloseTimeout( ),
                                activityType.getDefaultTaskScheduleToCloseTimeout( ),
                                DEFAULT_SCHEDULE_TO_CLOSE_TIMEOUT_UNDEFINED ),
                            parseActivityPeriod(
                                scheduleActivity.getScheduleToStartTimeout( ),
                                activityType.getDefaultTaskScheduleToStartTimeout( ),
                                DEFAULT_SCHEDULE_TO_START_TIMEOUT_UNDEFINED ),
                            parseActivityPeriod(
                                scheduleActivity.getStartToCloseTimeout( ),
                                activityType.getDefaultTaskStartToCloseTimeout( ),
                                DEFAULT_START_TO_CLOSE_TIMEOUT_UNDEFINED ),
                            parseActivityPeriod(
                                scheduleActivity.getHeartbeatTimeout() ,
                                activityType.getDefaultTaskHeartbeatTimeout( ),
                                DEFAULT_HEARTBEAT_TIMEOUT_UNDEFINED )
                        ) );
                        activityTaskScheduledCount++;

                        notificationTypeListPairs.add( Pair.pair( "activity", list ) );
                      } catch ( final ScheduleActivityTaskException e ) {
                        workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                            workflowExecution,
                            new ScheduleActivityTaskFailedEventAttributes( )
                                .withActivityId( scheduleActivity.getActivityId( ) )
                                .withActivityType( scheduleActivity.getActivityType( ) )
                                .withCause( e.getFailedCause( ) )
                                .withDecisionTaskCompletedEventId( completedId )
                        ) );
                        scheduleDecisionTask = true;
                      } catch ( final Exception e ) {
                        throw up( e );
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
                    case "ScheduleLambdaFunction":
                      final ScheduleLambdaFunctionDecisionAttributes scheduleLambdaFunction =
                          decision.getScheduleLambdaFunctionDecisionAttributes();
                      workflowExecution.addHistoryEvent(WorkflowHistoryEvent.create(
                          workflowExecution,
                          new ScheduleLambdaFunctionFailedEventAttributes()
                          .withId(scheduleLambdaFunction.getId())
                          .withName(scheduleLambdaFunction.getName())
                          .withCause( ScheduleLambdaFunctionFailedCause.LAMBDA_SERVICE_NOT_AVAILABLE_IN_REGION )
                          .withDecisionTaskCompletedEventId( completedId )));
                      scheduleDecisionTask = true;
                      break;
                    case "StartTimer":
                      final StartTimerDecisionAttributes startTimer = decision.getStartTimerDecisionAttributes( );
                      try {
                        if ( !timers.listByExample(
                            Timer.exampleWithTimerId(
                                accountFullName,
                                workflowExecution.getDomainName(),
                                workflowExecution.getDisplayName(),
                                startTimer.getTimerId() ),
                            Predicates.alwaysTrue(),
                            Functions.identity()
                        ).isEmpty() ) {
                          throw new StartTimerException( StartTimerFailedCause.TIMER_ID_ALREADY_IN_USE );
                        }
                        final Long startedId = workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                            workflowExecution,
                            new TimerStartedEventAttributes()
                                .withControl( startTimer.getControl() )
                                .withDecisionTaskCompletedEventId( completedId )
                                .withStartToFireTimeout( startTimer.getStartToFireTimeout() )
                                .withTimerId( startTimer.getTimerId() )
                        ) );
                        if ( timers.countByWorkflowExecution(
                            accountFullName,
                            domain.getDisplayName(),
                            workflowExecution.getDisplayName() ) >=
                            SimpleWorkflowProperties.getOpenTimersPerWorkflowExecution() ) {
                          throw new StartTimerException( StartTimerFailedCause.OPEN_TIMERS_LIMIT_EXCEEDED );
                        }
                        timers.save( Timer.create(
                            userFullName,
                            workflowExecution,
                            workflowExecution.getDomainName(),
                            workflowExecution.getDomainUuid(),
                            startTimer.getTimerId(),
                            startTimer.getControl(),
                            parsePeriod( startTimer.getStartToFireTimeout(), 0 ),
                            completedId,
                            startedId
                        ) );
                      } catch ( StartTimerException e ) {
                        workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                            workflowExecution,
                            new StartTimerFailedEventAttributes()
                                .withCause( e.getFailedCause( ) )
                                .withDecisionTaskCompletedEventId( completedId )
                                .withTimerId( startTimer.getTimerId() )
                        ) );
                        scheduleDecisionTask = true;
                      } catch ( SwfMetadataException e ) {
                        throw up( e );
                      }
                      break;
                    default:
                      throw up( new SimpleWorkflowException(
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
                  notificationTypeListPairs.add( Pair.pair( "decision", workflowExecution.getTaskList( ) ) );
                } else {
                  workflowExecution.updateTimeStamps( );
                }
              }
              return workflowExecution;
            }
          } );
          }

          //TODO:STEVE: update API to allow batch notification
          for ( final Pair<String,String> notificationTypeListPair : notificationTypeListPairs ) {
            notifyTaskList(
                accountFullName,
                domain.getDisplayName( ),
                notificationTypeListPair.getLeft( ),
                notificationTypeListPair.getRight( ) );
          }
    } catch( Exception e ) {
      throw handleException( e );
    }

    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public SimpleWorkflowMessage signalWorkflowExecution( final SignalWorkflowExecutionRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    try {
      final WorkflowExecution example = WorkflowExecution.exampleForOpenWorkflow(
          accountFullName,
          request.getDomain(),
          request.getWorkflowId(),
          request.getRunId() );
      final Pair<String,String> domainUuidRunIdPair = workflowExecutions.lookupByExample(
          example, accountFullName,
          request.getWorkflowId( ),
          Predicates.alwaysTrue( ),
          Pair.builder(
              WorkflowExecutions.WorkflowExecutionStringFunctions.DOMAIN_UUID,
              SimpleWorkflowMetadatas.toDisplayName( ) ) );

      final Pair<String,String> domainTaskListPair;
      try ( final WorkflowLock lock = WorkflowLock.lock( accountFullName, domainUuidRunIdPair ) ) {
        domainTaskListPair = workflowExecutions.withRetries().updateByExample(
            example,
            accountFullName,
            request.getWorkflowId(),
            workflowExecution -> {
              if ( accessible.apply( workflowExecution ) ) {
                workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                    workflowExecution,
                    new WorkflowExecutionSignaledEventAttributes()
                        .withExternalInitiatedEventId( 0L )
                        .withInput( request.getInput() )
                        .withSignalName( request.getSignalName() )
                ) );
                if ( workflowExecution.getDecisionStatus() != Pending ) {
                  workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                      workflowExecution,
                      new DecisionTaskScheduledEventAttributes()
                          .withTaskList( new TaskList().withName( workflowExecution.getTaskList() ) )
                          .withStartToCloseTimeout( String.valueOf( workflowExecution.getTaskStartToCloseTimeout() ) )
                  ) );
                  if ( workflowExecution.getDecisionStatus() == Idle ) {
                    workflowExecution.setDecisionStatus( Pending );
                    workflowExecution.setDecisionTimestamp( new Date() );
                    return Pair.pair( workflowExecution.getDomainName(), workflowExecution.getTaskList() );
                  }
                }
              }
              return null;
            }
        );
      }

      if ( domainTaskListPair != null ) {
        notifyTaskList( accountFullName, domainTaskListPair.getLeft( ), "decision", domainTaskListPair.getRight( ) );
      }
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          request.getRunId( ) == null ?
              "Unknown execution, workflowId = " + request.getWorkflowId( ) :
              "Unknown execution: WorkflowExecution=[workflowId=" + request.getWorkflowId( ) + ", runId="+ request.getRunId( ) +"]" );
    } catch ( SwfMetadataException e ) {
      throw handleException( e );
    }

    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public SimpleWorkflowMessage requestCancelWorkflowExecution( final RequestCancelWorkflowExecutionRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    try {
      final WorkflowExecution example = WorkflowExecution.exampleForOpenWorkflow(
          accountFullName,
          request.getDomain(),
          request.getWorkflowId(),
          request.getRunId() );
      final Pair<String,String> domainUuidRunIdPair = workflowExecutions.lookupByExample(
          example, accountFullName,
          request.getWorkflowId( ),
          Predicates.alwaysTrue( ),
          Pair.builder(
              WorkflowExecutions.WorkflowExecutionStringFunctions.DOMAIN_UUID,
              SimpleWorkflowMetadatas.toDisplayName( ) ) );

      final Pair<String, String> domainTaskListPair;
      try ( final WorkflowLock lock = WorkflowLock.lock( accountFullName,  domainUuidRunIdPair ) ) {
        domainTaskListPair = workflowExecutions.withRetries().updateByExample(
            example,
            accountFullName,
            request.getWorkflowId(),
            workflowExecution -> {
              if ( accessible.apply( workflowExecution ) ) {
                workflowExecution.setCancelRequested( true );
                workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                    workflowExecution,
                    new WorkflowExecutionCancelRequestedEventAttributes()
                        .withExternalInitiatedEventId( 0L )
                ) );
                if ( workflowExecution.getDecisionStatus() != Pending ) {
                  workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create(
                      workflowExecution,
                      new DecisionTaskScheduledEventAttributes()
                          .withTaskList( new TaskList().withName( workflowExecution.getTaskList() ) )
                          .withStartToCloseTimeout( String.valueOf( workflowExecution.getTaskStartToCloseTimeout() ) )
                  ) );
                  if ( workflowExecution.getDecisionStatus() == Idle ) {
                    workflowExecution.setDecisionStatus( Pending );
                    workflowExecution.setDecisionTimestamp( new Date() );
                    return Pair.pair( workflowExecution.getDomainName(), workflowExecution.getTaskList() );
                  }
                }
              }
              return null;
            }
        );
      }

      if ( domainTaskListPair != null ) {
        notifyTaskList( accountFullName, domainTaskListPair.getLeft( ), "decision", domainTaskListPair.getRight( ) );
      }
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          request.getRunId( ) == null ?
              "Unknown execution, workflowId = " + request.getWorkflowId( ) :
              "Unknown execution: WorkflowExecution=[workflowId=" + request.getWorkflowId( ) + ", runId="+ request.getRunId( ) +"]" );
    } catch ( SwfMetadataException e ) {
      throw handleException( e );
    }

    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public SimpleWorkflowMessage terminateWorkflowExecution( final TerminateWorkflowExecutionRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    try {
      final WorkflowExecution example = WorkflowExecution.exampleForOpenWorkflow(
          accountFullName,
          request.getDomain(),
          request.getWorkflowId(),
          request.getRunId() );
      final Pair<String,String> domainUuidRunIdPair = workflowExecutions.lookupByExample(
          example, accountFullName,
          request.getWorkflowId( ),
          Predicates.alwaysTrue( ),
          Pair.builder(
              WorkflowExecutions.WorkflowExecutionStringFunctions.DOMAIN_UUID,
              SimpleWorkflowMetadatas.toDisplayName( ) ) );

      try ( final WorkflowLock lock = WorkflowLock.lock( accountFullName, domainUuidRunIdPair ) ) {
        workflowExecutions.withRetries().updateByExample(
            example,
            accountFullName,
            request.getWorkflowId(),
            (Function<WorkflowExecution, Void>) workflowExecution -> {
              if ( accessible.apply( workflowExecution ) ) {
                workflowExecution.closeWorkflow(
                    WorkflowExecution.CloseStatus.Terminated,
                    WorkflowHistoryEvent.create(
                        workflowExecution,
                        new WorkflowExecutionTerminatedEventAttributes()
                            .withChildPolicy( MoreObjects.firstNonNull(
                                request.getChildPolicy(),
                                workflowExecution.getChildPolicy() ) )
                            .withDetails( request.getDetails() )
                            .withReason( request.getReason() ) )
                );
              }
              deleteActivities( activityTasks, accountFullName, workflowExecution );
              return null;
            }
        );
      }
    } catch ( SwfMetadataNotFoundException e ) {
      throw new SimpleWorkflowClientException(
          "UnknownResourceFault",
          request.getRunId( ) == null ?
          "Unknown execution, workflowId = " + request.getWorkflowId( ) :
          "Unknown execution: WorkflowExecution=[workflowId=" + request.getWorkflowId( ) + ", runId="+ request.getRunId( ) +"]" );
    } catch ( SwfMetadataException e ) {
      throw handleException( e );
    }

    return request.reply( new SimpleWorkflowMessage( ) );
  }

  public History getWorkflowExecutionHistory( final GetWorkflowExecutionHistoryRequest request ) throws SimpleWorkflowException {
    final Context ctx = Contexts.lookup();
    final UserFullName userFullName = ctx.getUserFullName( );
    final AccountFullName accountFullName = userFullName.asAccountFullName();
    final Predicate<? super WorkflowExecution> accessible =
        SimpleWorkflowMetadatas.filteringFor( WorkflowExecution.class ).byPrivileges( ).buildPredicate( );

    final History history;
    try {
      history = workflowExecutions.lookupByExample(
          WorkflowExecution.exampleWithName( accountFullName, request.getExecution().getRunId( ) ),
          accountFullName,
          request.getExecution().getRunId(),
          accessible,
          workflowExecution -> {
            final List<WorkflowHistoryEvent> events = workflowExecution.getWorkflowHistory();
            final List<WorkflowHistoryEvent> reverseEvents = Lists.reverse( events );
            return new History( )
                .withEvents( Collections2.transform(
                    MoreObjects.firstNonNull( request.isReverseOrder( ), Boolean.FALSE ) ? reverseEvents : events,
                    TypeMappers.lookup( WorkflowHistoryEvent.class, HistoryEvent.class )
                ) );
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

  @SuppressWarnings( "WeakerAccess" )
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
      throw up( e );
    }
  }

  private static void buildFilters( final ClosedWorkflowExecutionFilterParameters parameters,
                                    final Conjunction filter,
                                    final Map<String,String> aliases ) {
    if ( parameters.getCloseStatusFilter( ) != null ) {
      filter.add( Restrictions.eq( "closeStatus", WorkflowExecution.CloseStatus.fromString( parameters.getCloseStatusFilter().getStatus( ) ) ) );
    }
    if ( parameters.getCloseTimeFilter( ) != null ) {
      if ( parameters.getCloseTimeFilter( ).getOldestDate( ) != null ) {
        filter.add( Restrictions.ge( "closeTimestamp", parameters.getCloseTimeFilter( ).getOldestDate( ) ) );
      }
      if ( parameters.getCloseTimeFilter( ).getLatestDate( ) != null ) {
        filter.add( Restrictions.le( "closeTimestamp", parameters.getCloseTimeFilter( ).getLatestDate( ) ) );
      }
    }
    buildFilters( (WorkflowExecutionFilterParameters) parameters, filter, aliases );
  }

  private static void buildFilters( final WorkflowExecutionFilterParameters parameters,
                                    final Conjunction filter,
                                    final Map<String,String> aliases ) {
    if ( parameters.getExecutionFilter( ) != null ) {
      filter.add( Restrictions.eq( "workflowId", parameters.getExecutionFilter( ).getWorkflowId( ) ) );
    }
    if ( parameters.getStartTimeFilter( ) != null ) {
      if ( parameters.getStartTimeFilter().getOldestDate( ) != null ) {
        filter.add( Restrictions.ge( "creationTimestamp", parameters.getStartTimeFilter().getOldestDate( ) ) );
      }
      if ( parameters.getStartTimeFilter().getLatestDate( ) != null ) {
        filter.add( Restrictions.le( "creationTimestamp", parameters.getStartTimeFilter().getLatestDate( ) ) );
      }
    }
    if ( parameters.getTagFilter( ) != null ) {
      aliases.put( "tagList", "tag" );
      filter.add( Restrictions.eq( "tag.elements", parameters.getTagFilter( ).getTag( ) ) );
    }
    if ( parameters.getTypeFilter( ) != null ) {
      if ( parameters.getTypeFilter( ).getName() != null ) {
        aliases.put( "workflowType", "workflowType" );
        filter.add( Restrictions.eq( "workflowType.displayName", parameters.getTypeFilter( ).getName() ) );
      }
      if ( parameters.getTypeFilter( ).getVersion( ) != null ) {
        aliases.put( "workflowType", "workflowType" );
        filter.add( Restrictions.eq( "workflowType.workflowVersion", parameters.getTypeFilter().getVersion() ) );
      }
    }
  }

  /**
   * Method always throws, signature allows use of "throw up ..."
   */
  private static RuntimeException up( final Throwable throwable ) {
    throw Exceptions.toUndeclared( throwable );
  }

  /**
   * Method always throws, signature allows use of "throw upClient ..."
   */
  private static RuntimeException upClient( final String errorCode,
                                            final String message ) {
    throw up( new SimpleWorkflowClientException( errorCode, message ) );
  }

  /**
   * Method always throws, signature allows use of "throw handleException ..."
   */
  private SimpleWorkflowException handleException( final Exception e ) throws SimpleWorkflowException {
    final SimpleWorkflowException cause = Exceptions.findCause( e, SimpleWorkflowException.class );
    if ( cause != null ) {
      throw cause;
    }

    final WorkflowHistorySizeLimitException historySizeLimitCause =
        Exceptions.findCause( e, WorkflowHistorySizeLimitException.class );
    if ( historySizeLimitCause != null ) {
      WorkflowExecutions.Utils.terminateWorkflowExecution(
          workflowExecutions,
          "EVENT_LIMIT_EXCEEDED",
          historySizeLimitCause.getAccountNumber( ),
          historySizeLimitCause.getDomain( ),
          historySizeLimitCause.getWorkflowId( ) );
      throw new SimpleWorkflowClientException( "LimitExceededFault", "Request would exceed history limit for workflow execution" );
    }

    final AuthQuotaException quotaCause = Exceptions.findCause( e, AuthQuotaException.class );
    if ( quotaCause != null ) {
      throw new SimpleWorkflowClientException( "LimitExceededFault", "Request would exceed quota for type: " + quotaCause.getType( ) );
    }

    final TaskTokenException tokenCause = Exceptions.findCause( e, TaskTokenException.class );
    if ( tokenCause != null ) {
      throw new SimpleWorkflowClientException( "InvalidParameterValue", "Invalid task token." );
    }

    // Amazon SWF client retries either InternalError or ThrottlingException after a backoff
    if ( PersistenceExceptions.isLockError( e ) || PersistenceExceptions.isStaleUpdate( e ) ) {
      throw new SimpleWorkflowClientException( "ThrottlingException", "Request throttled" );
    }

    logger.error( e, e );

    final SimpleWorkflowException exception = new SimpleWorkflowException( "InternalError", Role.Receiver, String.valueOf(e.getMessage( )) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
      exception.initCause( e );
    }
    throw exception;
  }

  private static Integer parsePeriod( final String period, final Integer noneValue ) {
    if ( period == null ) {
      return null;
    } else if ( "NONE".equals( period ) ) {
      return noneValue;
    } else {
      return Integer.parseInt( period );
    }
  }

  private static Integer parseActivityPeriod(
      final String period,
      final Integer defaultValue,
      final ScheduleActivityTaskFailedCause failureOnNoDefault
  ) throws ScheduleActivityTaskException {
    if ( period == null && defaultValue == null ) {
      throw new ScheduleActivityTaskException( failureOnNoDefault );
    } else if ( period == null ) {
      return defaultValue < 0 ? null : defaultValue;
    } else if ( "NONE".equals( period ) ) {
      return null;
    } else {
      return Integer.parseInt( period );
    }
  }

  private static Integer requireDefault( final Integer value,
                                         final Integer defaultValue,
                                         final String description ) throws SimpleWorkflowClientException {
    if ( value == null && defaultValue == null ) {
      throw new SimpleWorkflowClientException(
          "DefaultUndefinedFault" ,
          description + " is required" );
    }
    return MoreObjects.firstNonNull( value, defaultValue );
  }

  private static <T> Iterable<List<T>> shufflePartitions( final List<T> pending ) {
    return Iterables.transform(
        Iterables.partition( pending, 40 ),
        segment -> { List<T> copy = Lists.newArrayList( segment ); Collections.shuffle( copy ); return copy; } );
  }

  private static void noteTaskListActivity( final AccountFullName accountFullName,
                                            final String domain,
                                            final String type,
                                            final String taskList ) {
    taskListActivity.put( NotifyTaskList.of( accountFullName, domain, type, taskList ), System.currentTimeMillis( ) );
  }

  private static boolean taskListActive( final AccountFullName accountFullName,
                                         final String domain,
                                         final String type,
                                         final String taskList,
                                         final long timestamp ) {
    return taskListActivity.getOrDefault( NotifyTaskList.of( accountFullName, domain, type, taskList ), 0L ) > timestamp;
  }

  private static void taskListActivityCleanup( long timestamp ) {
    for ( final Map.Entry<NotifyTaskList,Long> entry : taskListActivity.entrySet( ) ) {
      if ( entry.getValue( ) < timestamp ) {
        taskListActivity.remove( entry.getKey( ), entry.getValue( ) );
      }
    }
  }

  private static void notifyTaskList( final AccountFullName accountFullName,
                                      final String domain,
                                      final String type,
                                      final String taskList ) {
    noteTaskListActivity( accountFullName, domain, type, taskList );
    NotifyClient.notifyTaskList( accountFullName, domain, type, taskList );
  }

  private static final long EXPIRY_MILLIS = TimeUnit.SECONDS.toMillis( 30 );

  private static <R extends SimpleWorkflowMessage> R handleTaskPolling( final AccountFullName accountFullName,
                                         final String domain,
                                         final String type,
                                         final String taskList,
                                         final String correlationId,
                                         final R emptyResponse,
                                         final Callable<R> responseCallable) {
    final Long pollTimeout = System.currentTimeMillis() + EXPIRY_MILLIS;
    return handleTaskPolling(accountFullName, domain, type, taskList, correlationId, emptyResponse, responseCallable, true, pollTimeout);
  }

  private static <R extends SimpleWorkflowMessage> R handleTaskPolling( final AccountFullName accountFullName,
                                         final String domain,
                                         final String type,
                                         final String taskList,
                                         final String correlationId,
                                         final R emptyResponse,
                                         final Callable<R> responseCallable,
                                         final boolean checkImmediately,
                                         final long pollTimeout) {
    final String list = Joiner.on('/').join( type, domain, taskList );
    try {
        final long activityTimestamp = System.currentTimeMillis( ) - TimeUnit.SECONDS.toMillis( 1L );
        if ( checkImmediately && taskListActive( accountFullName, domain, type, taskList, activityTimestamp ) ) {
          final R taskResponse = responseCallable.call( );
          if ( taskResponse != null ) {
            taskResponse.setCorrelationId(correlationId);
            return taskResponse;
          }
        }
        NotifyClient.pollTaskList(accountFullName, domain, type, taskList, pollTimeout, Contexts.consumerWithCurrentContext(
                (notified) -> {
                  try {
                    if (notified) {
                      final SimpleWorkflowMessage taskResponse = responseCallable.call();
                      if (taskResponse != null) {
                        taskResponse.setCorrelationId(correlationId);
                        Contexts.response(taskResponse);
                        return;
                      } else if ( System.currentTimeMillis() < pollTimeout ) {
                        handleTaskPolling( accountFullName, domain, type, taskList, correlationId, emptyResponse, responseCallable, false, pollTimeout );
                        return;
                      }
                    }

                    emptyResponse.setCorrelationId(correlationId);
                    Contexts.response(emptyResponse);
                  } catch (final InterruptedException e) {
                    logger.info("Interrupted while polling for task " + list, e);
                  } catch (final Exception e) {
                    logger.error("Error polling for task " + list, e);
                  }
                }
        ));
        return null;
    } catch ( Exception e ) {
      logger.error( "Error polling for task " + list, e );
      emptyResponse.setCorrelationId( correlationId );
      return emptyResponse;
    }
  }

  public static class SimpleWorkflowServiceActivityCleanup implements EventListener<ClockTick> {

    public static void register() {
      Listeners.register( ClockTick.class, new SimpleWorkflowServiceActivityCleanup( ) );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      taskListActivityCleanup( System.currentTimeMillis( ) - TimeUnit.SECONDS.toMillis( 10 ) );
    }
  }

  private static final class ScheduleActivityTaskException extends Exception {
    private static final long serialVersionUID = 1L;

    private final ScheduleActivityTaskFailedCause failedCause;

    ScheduleActivityTaskException( final ScheduleActivityTaskFailedCause failedCause ) {
      this.failedCause = failedCause;
    }

    ScheduleActivityTaskFailedCause getFailedCause() {
      return failedCause;
    }
  }

  private static final class StartTimerException extends Exception {
    private static final long serialVersionUID = 1L;

    private final StartTimerFailedCause failedCause;

    StartTimerException( final StartTimerFailedCause failedCause ) {
      this.failedCause = failedCause;
    }

    StartTimerFailedCause getFailedCause() {
      return failedCause;
    }
  }
}
