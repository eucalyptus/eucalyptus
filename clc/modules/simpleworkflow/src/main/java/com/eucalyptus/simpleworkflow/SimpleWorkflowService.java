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

import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.AuthQuotaException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadatas;
import com.eucalyptus.simpleworkflow.common.model.ActivityTask;
import com.eucalyptus.simpleworkflow.common.model.ActivityTypeDetail;
import com.eucalyptus.simpleworkflow.common.model.ActivityTypeInfo;
import com.eucalyptus.simpleworkflow.common.model.ActivityTypeInfos;
import com.eucalyptus.simpleworkflow.common.model.DecisionTask;
import com.eucalyptus.simpleworkflow.common.model.DeprecateActivityTypeRequest;
import com.eucalyptus.simpleworkflow.common.model.DeprecateDomainRequest;
import com.eucalyptus.simpleworkflow.common.model.DeprecateWorkflowTypeRequest;
import com.eucalyptus.simpleworkflow.common.model.DescribeActivityTypeRequest;
import com.eucalyptus.simpleworkflow.common.model.DescribeDomainRequest;
import com.eucalyptus.simpleworkflow.common.model.DescribeWorkflowTypeRequest;
import com.eucalyptus.simpleworkflow.common.model.DomainDetail;
import com.eucalyptus.simpleworkflow.common.model.DomainInfo;
import com.eucalyptus.simpleworkflow.common.model.DomainInfos;
import com.eucalyptus.simpleworkflow.common.model.ListActivityTypesRequest;
import com.eucalyptus.simpleworkflow.common.model.ListDomainsRequest;
import com.eucalyptus.simpleworkflow.common.model.ListWorkflowTypesRequest;
import com.eucalyptus.simpleworkflow.common.model.PollForActivityTaskRequest;
import com.eucalyptus.simpleworkflow.common.model.PollForDecisionTaskRequest;
import com.eucalyptus.simpleworkflow.common.model.RegisterActivityTypeRequest;
import com.eucalyptus.simpleworkflow.common.model.RegisterDomainRequest;
import com.eucalyptus.simpleworkflow.common.model.RegisterWorkflowTypeRequest;
import com.eucalyptus.simpleworkflow.common.model.Run;
import com.eucalyptus.simpleworkflow.common.model.SimpleWorkflowMessage;
import com.eucalyptus.simpleworkflow.common.model.StartWorkflowExecutionRequest;
import com.eucalyptus.simpleworkflow.common.model.WorkflowTypeDetail;
import com.eucalyptus.simpleworkflow.common.model.WorkflowTypeInfo;
import com.eucalyptus.simpleworkflow.common.model.WorkflowTypeInfos;
import com.eucalyptus.simpleworkflow.persist.PersistenceActivityTypes;
import com.eucalyptus.simpleworkflow.persist.PersistenceDomains;
import com.eucalyptus.simpleworkflow.persist.PersistenceWorkflowTypes;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedType;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.ws.Role;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

/**
 *
 */
public class SimpleWorkflowService {

  private static final Logger logger = Logger.getLogger( SimpleWorkflowService.class );

  private final Domains domains = new PersistenceDomains( );
  private final ActivityTypes activityTypes = new PersistenceActivityTypes( );
  private final WorkflowTypes workflowTypes = new PersistenceWorkflowTypes( );

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
              parsePeriod( request.getDefaultExecutionStartToCloseTimeout( ) ),
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


  public Run startWorkflowExecution( final StartWorkflowExecutionRequest request ) throws SimpleWorkflowException {
    final Run run = new Run( );
    run.setRunId( UUID.randomUUID( ).toString( ) );
    return request.reply( run );
  }

  public ActivityTask pollForActivityTask( final PollForActivityTaskRequest request ) throws SimpleWorkflowException {
    final ActivityTask activityTask = new ActivityTask( );
    longPollSimulation( );
    return request.reply( activityTask );
  }

  public DecisionTask pollForDecisionTask( final PollForDecisionTaskRequest request ) throws SimpleWorkflowException {
    final DecisionTask decisionTask = new DecisionTask( );
    longPollSimulation( );
    return request.reply( decisionTask );
  }

  private <T extends AbstractPersistent & RestrictedType> T allocate(
      final Supplier<T> allocator,
      final Class<T> type,
      final String name
  ) throws SimpleWorkflowException {
    try {
      return RestrictedTypes.allocateUnitlessResources( type, 1, transactional( allocator ) ).get( 0 );
    } catch ( Exception e ) {
      final ConstraintViolationException constraintViolationException =
          Exceptions.findCause( e, ConstraintViolationException.class );
      if ( constraintViolationException != null ) {
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

  protected <E extends AbstractPersistent,P> Function<P,E> transactional( final Function<P,E> function ) {
    return Entities.asTransaction( function );
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

  private static void longPollSimulation( ) {
    try {
      Thread.sleep( 5000L );
    } catch ( InterruptedException e ) {
      // respond immediately
    }
  }
}
