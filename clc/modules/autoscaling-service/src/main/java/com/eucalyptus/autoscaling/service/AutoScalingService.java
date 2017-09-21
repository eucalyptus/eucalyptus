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
package com.eucalyptus.autoscaling.service;

import com.eucalyptus.auth.AuthContextSupplier;
import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.autoscaling.common.AutoScalingBackend;
import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
import com.eucalyptus.autoscaling.common.internal.activities.ScalingActivities;
import com.eucalyptus.autoscaling.common.internal.activities.ScalingActivity;
import com.eucalyptus.autoscaling.common.internal.configurations.LaunchConfiguration;
import com.eucalyptus.autoscaling.common.internal.configurations.LaunchConfigurations;
import com.eucalyptus.autoscaling.common.internal.groups.AutoScalingGroup;
import com.eucalyptus.autoscaling.common.internal.groups.AutoScalingGroupMinimumView;
import com.eucalyptus.autoscaling.common.internal.groups.AutoScalingGroups;
import com.eucalyptus.autoscaling.common.internal.groups.MetricCollectionType;
import com.eucalyptus.autoscaling.common.internal.groups.ScalingProcessType;
import com.eucalyptus.autoscaling.common.internal.groups.TerminationPolicyType;
import com.eucalyptus.autoscaling.common.internal.instances.AutoScalingInstance;
import com.eucalyptus.autoscaling.common.internal.instances.AutoScalingInstances;
import com.eucalyptus.autoscaling.common.internal.metadata.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.common.internal.metadata.AutoScalingMetadataNotFoundException;
import com.eucalyptus.autoscaling.common.internal.policies.AdjustmentType;
import com.eucalyptus.autoscaling.common.internal.policies.ScalingPolicies;
import com.eucalyptus.autoscaling.common.internal.tags.Tag;
import com.eucalyptus.autoscaling.common.internal.tags.TagSupport;
import com.eucalyptus.autoscaling.common.internal.tags.Tags;
import com.eucalyptus.autoscaling.common.msgs.Activity;
import com.eucalyptus.autoscaling.common.msgs.AdjustmentTypes;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingInstanceDetails;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingMessage;
import com.eucalyptus.autoscaling.common.msgs.DeleteNotificationConfigurationResponseType;
import com.eucalyptus.autoscaling.common.msgs.DeleteNotificationConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.DeleteScheduledActionResponseType;
import com.eucalyptus.autoscaling.common.msgs.DeleteScheduledActionType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAccountLimitsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAccountLimitsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAdjustmentTypesResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAdjustmentTypesType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingInstancesResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingInstancesType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingNotificationTypesResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingNotificationTypesType;
import com.eucalyptus.autoscaling.common.msgs.DescribeLaunchConfigurationsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeLaunchConfigurationsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeMetricCollectionTypesResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeMetricCollectionTypesType;
import com.eucalyptus.autoscaling.common.msgs.DescribeNotificationConfigurationsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeNotificationConfigurationsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeScalingActivitiesResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeScalingActivitiesType;
import com.eucalyptus.autoscaling.common.msgs.DescribeScalingProcessTypesResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeScalingProcessTypesType;
import com.eucalyptus.autoscaling.common.msgs.DescribeScheduledActionsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeScheduledActionsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeTagsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeTagsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeTerminationPolicyTypesResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeTerminationPolicyTypesType;
import com.eucalyptus.autoscaling.common.msgs.Filter;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.MetricCollectionTypes;
import com.eucalyptus.autoscaling.common.msgs.MetricGranularityType;
import com.eucalyptus.autoscaling.common.msgs.MetricGranularityTypes;
import com.eucalyptus.autoscaling.common.msgs.ProcessType;
import com.eucalyptus.autoscaling.common.msgs.PutNotificationConfigurationResponseType;
import com.eucalyptus.autoscaling.common.msgs.PutNotificationConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.PutScheduledUpdateGroupActionResponseType;
import com.eucalyptus.autoscaling.common.msgs.PutScheduledUpdateGroupActionType;
import com.eucalyptus.autoscaling.common.msgs.ResponseMetadata;
import com.eucalyptus.autoscaling.common.msgs.TagDescription;
import com.eucalyptus.autoscaling.common.msgs.TagDescriptionList;
import com.eucalyptus.autoscaling.common.policy.AutoScalingPolicySpec;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.Role;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessages;

/**
 *
 */
@ComponentNamed
public class AutoScalingService {
  private static final Logger logger = Logger.getLogger( AutoScalingService.class );

  private static final Map<String,Function<Tag,String>> tagFilterExtractors =
      ImmutableMap.<String,Function<Tag,String>>builder()
          .put( "auto-scaling-group", Tags.resourceId() )
          .put( "key", Tags.key() )
          .put( "value", Tags.value() )
          .put( "propagate-at-launch", Tags.propagateAtLaunch() )
          .build();

  private static final Map<String,Function<String,String>> tagValuePreProcessors =
      ImmutableMap.<String,Function<String,String>>builder()
          .put( "propagate-at-launch", Strings.lower() )
          .build();

  private final LaunchConfigurations launchConfigurations;
  private final AutoScalingGroups autoScalingGroups;
  private final AutoScalingInstances autoScalingInstances;
  private final ScalingPolicies scalingPolicies;
  private final ScalingActivities scalingActivities;

  @Inject
  public AutoScalingService( final LaunchConfigurations launchConfigurations,
                                    final AutoScalingGroups autoScalingGroups,
                                    final AutoScalingInstances autoScalingInstances,
                                    final ScalingPolicies scalingPolicies,
                                    final ScalingActivities scalingActivities ) {
    this.launchConfigurations = launchConfigurations;
    this.autoScalingGroups = autoScalingGroups;
    this.autoScalingInstances = autoScalingInstances;
    this.scalingPolicies = scalingPolicies;
    this.scalingActivities = scalingActivities;
  }
  public DeleteNotificationConfigurationResponseType deleteNotificationConfiguration(
      final DeleteNotificationConfigurationType request
  ) throws EucalyptusCloudException {
    DeleteNotificationConfigurationResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteScheduledActionResponseType deleteScheduledAction(
      final DeleteScheduledActionType request
  ) throws EucalyptusCloudException {
    DeleteScheduledActionResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeAccountLimitsResponseType describeAccountLimits(
      final DescribeAccountLimitsType request
  ) throws EucalyptusCloudException {
    DescribeAccountLimitsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeAdjustmentTypesResponseType describeAdjustmentTypes( final DescribeAdjustmentTypesType request) throws EucalyptusCloudException {
    final DescribeAdjustmentTypesResponseType reply = request.getReply( );

    reply.getDescribeAdjustmentTypesResult().setAdjustmentTypes( new AdjustmentTypes(
        Collections2.transform(
            Collections2.filter( EnumSet.allOf( AdjustmentType.class ), RestrictedTypes.filterPrivilegedWithoutOwner() ),
            Strings.toStringFunction() ) ) );

    return reply;
  }

  public DescribeAutoScalingGroupsResponseType describeAutoScalingGroups( final DescribeAutoScalingGroupsType request ) throws EucalyptusCloudException {
    final DescribeAutoScalingGroupsResponseType reply = request.getReply( );

    //TODO: MaxRecords / NextToken support for DescribeAutoScalingGroups

    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.autoScalingGroupNames().remove( "verbose" ) || !request.autoScalingGroupNames( ).isEmpty( );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    final Predicate<AutoScalingMetadata.AutoScalingGroupMetadata> requestedAndAccessible =
        AutoScalingMetadatas.filterPrivilegesByIdOrArn( AutoScalingMetadata.AutoScalingGroupMetadata.class, request.autoScalingGroupNames() );

    try {
      final List<AutoScalingGroupType> results = reply.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember();
      results.addAll( autoScalingGroups.list(
          ownerFullName,
          requestedAndAccessible,
          TypeMappers.lookup( AutoScalingGroup.class, AutoScalingGroupType.class ) ) );

      final Map<String,List<Tag>> tagsMap = TagSupport.forResourceClass( AutoScalingGroup.class )
          .getResourceTagMap( ctx.getUserFullName().asAccountFullName(),
              Iterables.transform( results, AutoScalingGroupType.groupName() ), Predicates.alwaysTrue() );
      for ( final AutoScalingGroupType type : results ) {
        final TagDescriptionList tags = new TagDescriptionList();
        Tags.addFromTags( tags.getMember(), TagDescription.class, tagsMap.get( type.getAutoScalingGroupName() ) );
        if ( !tags.getMember().isEmpty() ) {
          type.setTags( tags );
        }
      }
    } catch ( Exception e ) {
      handleException( e );
    }

    return reply;
  }

  public DescribeAutoScalingInstancesResponseType describeAutoScalingInstances( final DescribeAutoScalingInstancesType request ) throws EucalyptusCloudException {
    final DescribeAutoScalingInstancesResponseType reply = request.getReply( );

    //TODO: MaxRecords / NextToken support for DescribeAutoScalingInstances

    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.instanceIds().remove( "verbose" ) || !request.instanceIds().isEmpty();
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    final Predicate<? super AutoScalingInstance> requestedAndAccessible =
        AutoScalingMetadatas.filteringFor(AutoScalingInstance.class)
            .byId( request.instanceIds() )
            .byPrivileges( )
            .buildPredicate( );

    try {
      final List<AutoScalingInstanceDetails> results =
          reply.getDescribeAutoScalingInstancesResult().getAutoScalingInstances().getMember();
      results.addAll( autoScalingInstances.list(
          ownerFullName,
          requestedAndAccessible,
          TypeMappers.lookup( AutoScalingInstance.class, AutoScalingInstanceDetails.class ) ) );
    } catch ( Exception e ) {
      handleException( e );
    }

    return reply;
  }

  public DescribeAutoScalingNotificationTypesResponseType describeAutoScalingNotificationTypes(
      final DescribeAutoScalingNotificationTypesType request
  ) throws EucalyptusCloudException {
    DescribeAutoScalingNotificationTypesResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeLaunchConfigurationsResponseType describeLaunchConfigurations( DescribeLaunchConfigurationsType request) throws EucalyptusCloudException {
    final DescribeLaunchConfigurationsResponseType reply = request.getReply( );

    //TODO: MaxRecords / NextToken support for DescribeLaunchConfigurations

    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.launchConfigurationNames().remove( "verbose" ) ||
        (!request.launchConfigurationNames().isEmpty() && Iterables.all( request.launchConfigurationNames( ), AutoScalingResourceName.isResourceName( ) ) );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    final Predicate<AutoScalingMetadata.LaunchConfigurationMetadata> requestedAndAccessible =
        AutoScalingMetadatas.filterPrivilegesByIdOrArn( AutoScalingMetadata.LaunchConfigurationMetadata.class, request.launchConfigurationNames() );

    try {
      final List<LaunchConfigurationType> results = reply.getDescribeLaunchConfigurationsResult( ).getLaunchConfigurations().getMember();
      results.addAll( launchConfigurations.list(
          ownerFullName,
          requestedAndAccessible,
          TypeMappers.lookup( LaunchConfiguration.class, LaunchConfigurationType.class ) ) );
    } catch ( Exception e ) {
      handleException( e );
    }

    return reply;
  }

  public DescribeMetricCollectionTypesResponseType describeMetricCollectionTypes( final DescribeMetricCollectionTypesType request ) throws EucalyptusCloudException {
    final DescribeMetricCollectionTypesResponseType reply = request.getReply( );

    reply.getDescribeMetricCollectionTypesResult().setMetrics( new MetricCollectionTypes(
        Collections2.transform(
            Collections2.filter( EnumSet.allOf( MetricCollectionType.class ), RestrictedTypes.filterPrivilegedWithoutOwner() ),
            Strings.toStringFunction() ) ) );
    reply.getDescribeMetricCollectionTypesResult().setGranularities( new MetricGranularityTypes(
        Collections.singletonList( new MetricGranularityType( "1Minute" ) )
    ) );

    return reply;
  }

  public DescribeNotificationConfigurationsResponseType describeNotificationConfigurations(
      final DescribeNotificationConfigurationsType request
  ) throws EucalyptusCloudException {
    DescribeNotificationConfigurationsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeScalingActivitiesResponseType describeScalingActivities( final DescribeScalingActivitiesType request ) throws EucalyptusCloudException {
    final DescribeScalingActivitiesResponseType reply = request.getReply( );
    final int maxRecords = Math.max( 1, MoreObjects.firstNonNull( request.getMaxRecords( ), Integer.MAX_VALUE ) );

    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.activityIds().remove( "verbose" ) ||
        AutoScalingResourceName.isResourceName().apply( request.getAutoScalingGroupName( ) );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final AutoScalingGroupMinimumView group = com.google.common.base.Strings.isNullOrEmpty( request.getAutoScalingGroupName() ) ?
          null :
          autoScalingGroups.lookup(
              ownerFullName,
              request.getAutoScalingGroupName(),
              TypeMappers.lookup( AutoScalingGroup.class, AutoScalingGroupMinimumView.class ) );

      final List<Activity> scalingActivities = this.scalingActivities.list(
          ownerFullName,
          group,
          request.activityIds(),
          AutoScalingMetadatas.filteringFor( ScalingActivity.class ).byPrivileges( ).buildPredicate( ),
          TypeMappers.lookup( ScalingActivity.class, Activity.class ) );
      Collections.sort( scalingActivities, Ordering.natural().reverse().onResultOf( Activity.startTime() ) );

      reply.getDescribeScalingActivitiesResult().getActivities().getMember().addAll(
          scalingActivities.size( ) > maxRecords ? scalingActivities.subList( 0, maxRecords ) : scalingActivities
      );
    } catch ( AutoScalingMetadataNotFoundException e ) {
      throw new AutoScalingClientException( "ValidationError", "Auto scaling group not found: " + request.getAutoScalingGroupName() );
    } catch ( AutoScalingMetadataException e ) {
      handleException( e );
    }

    return reply;
  }

  public DescribeScalingProcessTypesResponseType describeScalingProcessTypes( final DescribeScalingProcessTypesType request) throws EucalyptusCloudException {
    final DescribeScalingProcessTypesResponseType reply = request.getReply( );

    final List<ProcessType> policies = reply.getDescribeScalingProcessTypesResult().getProcesses().getMember();
    policies.addAll( Collections2.transform(
        Collections2.filter( EnumSet.allOf( ScalingProcessType.class ), RestrictedTypes.filterPrivilegedWithoutOwner() ),
        TypeMappers.lookup( ScalingProcessType.class, ProcessType.class )) );

    return reply;
  }

  public DescribeScheduledActionsResponseType describeScheduledActions(
      final DescribeScheduledActionsType request
  ) throws EucalyptusCloudException {
    DescribeScheduledActionsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeTagsResponseType describeTags( final DescribeTagsType request ) throws EucalyptusCloudException {
    final DescribeTagsResponseType reply = request.getReply( );

    //TODO: MaxRecords / NextToken support for DescribeTags

    final Collection<Predicate<Tag>> tagFilters = Lists.newArrayList();
    for ( final Filter filter : request.filters() ) {
      final Function<Tag,String> extractor = tagFilterExtractors.get( filter.getName() );
      if ( extractor == null ) {
        throw new AutoScalingClientException( "ValidationError",
            "Filter type "+filter.getName()+" is not correct. Allowed Filter types are: auto-scaling-group key value propagate-at-launch" );
      }
      final Function<String,String> tagValueConverter = MoreObjects.firstNonNull(
          tagValuePreProcessors.get( filter.getName() ),
          Functions.identity() );
      tagFilters.add( Predicates.compose(
          Predicates.in( Collections2.transform( filter.values(), tagValueConverter ) ),
          extractor ) );
    }

    final Context context = Contexts.lookup();

    final Ordering<Tag> ordering = Ordering.natural().onResultOf( Tags.resourceId() )
        .compound( Ordering.natural().onResultOf( Tags.key() ) )
        .compound( Ordering.natural().onResultOf( Tags.value() ) );
    try {
      final TagDescriptionList tagDescriptions = new TagDescriptionList();
      for ( final Tag tag : ordering.sortedCopy( Tags.list(
          context.getUserFullName().asAccountFullName(),
          Predicates.and( tagFilters ),
          Restrictions.conjunction(),
          Collections.emptyMap() ) ) ) {
        if ( Permissions.isAuthorized(
            AutoScalingPolicySpec.VENDOR_AUTOSCALING,
            tag.getResourceType(),
            tag.getKey(),
            context.getAccount(),
            PolicySpec.describeAction( AutoScalingPolicySpec.VENDOR_AUTOSCALING, tag.getResourceType() ),
            context.getAuthContext() ) ) {
          tagDescriptions.getMember().add( TypeMappers.transform( tag, TagDescription.class ) );
        }
      }
      if ( !tagDescriptions.getMember().isEmpty() ) {
        reply.getDescribeTagsResult().setTags( tagDescriptions );
      }
    } catch ( AutoScalingMetadataNotFoundException e ) {
      handleException( e );
    }

    return reply;
  }

  public DescribeTerminationPolicyTypesResponseType describeTerminationPolicyTypes( final DescribeTerminationPolicyTypesType request ) throws EucalyptusCloudException {
    final DescribeTerminationPolicyTypesResponseType reply = request.getReply( );

    final List<String> policies = reply.getDescribeTerminationPolicyTypesResult().getTerminationPolicyTypes().getMember();
    policies.addAll( Collections2.transform(
        Collections2.filter( EnumSet.allOf( TerminationPolicyType.class ), RestrictedTypes.filterPrivilegedWithoutOwner() ),
        Strings.toStringFunction() ) );

    return reply;
  }

  public PutNotificationConfigurationResponseType putNotificationConfiguration(
      final PutNotificationConfigurationType request
  ) throws EucalyptusCloudException {
    PutNotificationConfigurationResponseType reply = request.getReply( );
    return reply;
  }

  public PutScheduledUpdateGroupActionResponseType putScheduledUpdateGroupAction(
      final PutScheduledUpdateGroupActionType request
  ) throws EucalyptusCloudException {
    PutScheduledUpdateGroupActionResponseType reply = request.getReply( );
    return reply;
  }

  public AutoScalingMessage dispatchAction( final AutoScalingMessage request ) throws EucalyptusCloudException {
    final AuthContextSupplier user = Contexts.lookup( ).getAuthContext( );

    // Dispatch
    try {
      @SuppressWarnings( "unchecked" )
      final AutoScalingMessage backendRequest = (AutoScalingMessage) BaseMessages.deepCopy( request, getBackendMessageClass( request ) );
      final BaseMessage backendResponse = send( backendRequest );
      final AutoScalingMessage response = (AutoScalingMessage) BaseMessages.deepCopy( backendResponse, request.getReply().getClass() );
      final ResponseMetadata metadata = AutoScalingMessage.getResponseMetadata( response );
      if ( metadata != null ) {
        metadata.setRequestId( request.getCorrelationId( ) );
      }
      response.setCorrelationId( request.getCorrelationId( ) );
      return response;
    } catch ( Exception e ) {
      handleRemoteException( e );
      Exceptions.findAndRethrow( e, EucalyptusWebServiceException.class, EucalyptusCloudException.class );
      throw new EucalyptusCloudException( e );
    }
  }

  private static Class getBackendMessageClass( final BaseMessage request ) throws ClassNotFoundException {
    return Class.forName( request.getClass( ).getName( ).replace( ".common.msgs.", ".common.backend.msgs." ) );
  }

  private static BaseMessage send( final BaseMessage request ) throws Exception {
    try {
      return AsyncRequests.sendSyncWithCurrentIdentity( Topology.lookup( AutoScalingBackend.class ), request );
    } catch ( NoSuchElementException e ) {
      throw new AutoScalingUnavailableException( "Service Unavailable" );
    } catch ( final FailedRequestException e ) {
      if ( request.getReply( ).getClass( ).isInstance( e.getRequest( ) ) ) {
        return e.getRequest( );
      }
      throw e.getRequest( ) == null ?
          e :
          new AutoScalingException( "InternalError", Role.Receiver, "Internal error " + e.getRequest().getClass().getSimpleName() + ":false" );
    }
  }

  @SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
  private void handleRemoteException( final Exception e ) throws EucalyptusCloudException {
    final Optional<AsyncExceptions.AsyncWebServiceError> serviceErrorOption = AsyncExceptions.asWebServiceError( e );
    if ( serviceErrorOption.isPresent( ) ) {
      final AsyncExceptions.AsyncWebServiceError serviceError = serviceErrorOption.get( );
      final String code = serviceError.getCode( );
      final String message = serviceError.getMessage( );
      switch( serviceError.getHttpErrorCode( ) ) {
        case 400:
          throw new AutoScalingClientException( code, message );
        case 403:
          throw new AutoScalingAuthorizationException( code, message );
        case 503:
          throw new AutoScalingUnavailableException( message );
        default:
          throw new AutoScalingException( code, Role.Receiver, message );
      }
    }
  }

  private static void handleException( final Exception e ) throws AutoScalingException {
    handleException( e, false );
  }

  private static void handleException( final Exception e,
                                       final boolean isCreate ) throws AutoScalingException {
    final AutoScalingException cause = Exceptions.findCause( e, AutoScalingException.class );
    if ( cause != null ) {
      throw cause;
    }

    logger.error( e, e );

    final AutoScalingServiceException exception =
        new AutoScalingServiceException( "InternalFailure", String.valueOf(e.getMessage()) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );
    }
    throw exception;
  }
}
