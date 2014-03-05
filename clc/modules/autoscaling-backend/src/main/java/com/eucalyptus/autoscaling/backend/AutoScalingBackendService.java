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
package com.eucalyptus.autoscaling.backend;

import static com.eucalyptus.autoscaling.common.AutoScalingResourceName.InvalidResourceNameException;
import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingGroupMetadata;
import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.LaunchConfigurationMetadata;
import static com.eucalyptus.autoscaling.common.AutoScalingResourceName.Type.autoScalingGroup;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;

import com.eucalyptus.auth.AuthQuotaException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.autoscaling.activities.ActivityManager;
import com.eucalyptus.autoscaling.activities.PersistenceScalingActivities;
import com.eucalyptus.autoscaling.activities.ScalingActivities;
import com.eucalyptus.autoscaling.activities.ScalingActivity;
import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
import com.eucalyptus.autoscaling.common.backend.msgs.Activity;
import com.eucalyptus.autoscaling.common.backend.msgs.AdjustmentTypes;
import com.eucalyptus.autoscaling.common.backend.msgs.Alarms;
import com.eucalyptus.autoscaling.common.backend.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.backend.msgs.AutoScalingInstanceDetails;
import com.eucalyptus.autoscaling.common.backend.msgs.BlockDeviceMappingType;
import com.eucalyptus.autoscaling.common.backend.msgs.CreateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.CreateAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.backend.msgs.CreateLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.CreateLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.backend.msgs.CreateOrUpdateTagsResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.CreateOrUpdateTagsType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeleteAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeleteAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeleteLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeleteLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeleteNotificationConfigurationResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeleteNotificationConfigurationType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeletePolicyResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeletePolicyType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeleteScheduledActionResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeleteScheduledActionType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeleteTagsResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeleteTagsType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeAdjustmentTypesResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeAdjustmentTypesType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeAutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeAutoScalingInstancesResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeAutoScalingInstancesType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeAutoScalingNotificationTypesResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeAutoScalingNotificationTypesType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeLaunchConfigurationsResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeLaunchConfigurationsType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeMetricCollectionTypesResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeMetricCollectionTypesType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeNotificationConfigurationsResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeNotificationConfigurationsType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribePoliciesResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribePoliciesType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeScalingActivitiesResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeScalingActivitiesType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeScalingProcessTypesResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeScalingProcessTypesType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeScheduledActionsResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeScheduledActionsType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeTagsResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeTagsType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeTerminationPolicyTypesResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribeTerminationPolicyTypesType;
import com.eucalyptus.autoscaling.common.backend.msgs.DisableMetricsCollectionResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DisableMetricsCollectionType;
import com.eucalyptus.autoscaling.common.backend.msgs.EnableMetricsCollectionResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.EnableMetricsCollectionType;
import com.eucalyptus.autoscaling.common.backend.msgs.ExecutePolicyResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.ExecutePolicyType;
import com.eucalyptus.autoscaling.common.backend.msgs.Filter;
import com.eucalyptus.autoscaling.common.backend.msgs.LaunchConfigurationType;
import com.eucalyptus.autoscaling.common.backend.msgs.MetricCollectionTypes;
import com.eucalyptus.autoscaling.common.backend.msgs.ProcessType;
import com.eucalyptus.autoscaling.common.backend.msgs.PutNotificationConfigurationResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.PutNotificationConfigurationType;
import com.eucalyptus.autoscaling.common.backend.msgs.PutScalingPolicyResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.PutScalingPolicyType;
import com.eucalyptus.autoscaling.common.backend.msgs.PutScheduledUpdateGroupActionResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.PutScheduledUpdateGroupActionType;
import com.eucalyptus.autoscaling.common.backend.msgs.ResumeProcessesResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.ResumeProcessesType;
import com.eucalyptus.autoscaling.common.backend.msgs.ScalingPolicyType;
import com.eucalyptus.autoscaling.common.backend.msgs.SetDesiredCapacityResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.SetDesiredCapacityType;
import com.eucalyptus.autoscaling.common.backend.msgs.SetInstanceHealthResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.SetInstanceHealthType;
import com.eucalyptus.autoscaling.common.backend.msgs.SuspendProcessesResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.SuspendProcessesType;
import com.eucalyptus.autoscaling.common.backend.msgs.TagDescription;
import com.eucalyptus.autoscaling.common.backend.msgs.TagDescriptionList;
import com.eucalyptus.autoscaling.common.backend.msgs.TagType;
import com.eucalyptus.autoscaling.common.backend.msgs.TerminateInstanceInAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.TerminateInstanceInAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.backend.msgs.UpdateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.UpdateAutoScalingGroupType;
import com.eucalyptus.autoscaling.config.AutoScalingConfiguration;
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration;
import com.eucalyptus.autoscaling.configurations.LaunchConfigurationMinimumView;
import com.eucalyptus.autoscaling.configurations.LaunchConfigurations;
import com.eucalyptus.autoscaling.configurations.PersistenceLaunchConfigurations;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.autoscaling.groups.AutoScalingGroupCoreView;
import com.eucalyptus.autoscaling.groups.AutoScalingGroupMinimumView;
import com.eucalyptus.autoscaling.groups.AutoScalingGroups;
import com.eucalyptus.autoscaling.groups.MetricCollectionType;
import com.eucalyptus.autoscaling.groups.HealthCheckType;
import com.eucalyptus.autoscaling.groups.PersistenceAutoScalingGroups;
import com.eucalyptus.autoscaling.groups.ScalingProcessType;
import com.eucalyptus.autoscaling.groups.SuspendedProcess;
import com.eucalyptus.autoscaling.groups.TerminationPolicyType;
import com.eucalyptus.autoscaling.instances.AutoScalingInstance;
import com.eucalyptus.autoscaling.instances.AutoScalingInstanceGroupView;
import com.eucalyptus.autoscaling.instances.AutoScalingInstances;
import com.eucalyptus.autoscaling.instances.HealthStatus;
import com.eucalyptus.autoscaling.instances.PersistenceAutoScalingInstances;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataNotFoundException;
import com.eucalyptus.autoscaling.policies.AdjustmentType;
import com.eucalyptus.autoscaling.policies.PersistenceScalingPolicies;
import com.eucalyptus.autoscaling.policies.ScalingPolicies;
import com.eucalyptus.autoscaling.policies.ScalingPolicy;
import com.eucalyptus.autoscaling.policies.ScalingPolicyView;
import com.eucalyptus.autoscaling.tags.AutoScalingGroupTag;
import com.eucalyptus.autoscaling.tags.Tag;
import com.eucalyptus.autoscaling.tags.TagSupport;
import com.eucalyptus.autoscaling.tags.Tags;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Numbers;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

public class AutoScalingBackendService {
  private static final Logger logger = Logger.getLogger( AutoScalingBackendService.class );

  private static final Set<String> reservedPrefixes =
      ImmutableSet.<String>builder().add("aws:").add("euca:").build();

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

  public static long MAX_TAGS_PER_RESOURCE = 10;

  private final LaunchConfigurations launchConfigurations;
  private final AutoScalingGroups autoScalingGroups;
  private final AutoScalingInstances autoScalingInstances;
  private final ScalingPolicies scalingPolicies;
  private final ActivityManager activityManager;
  private final ScalingActivities scalingActivities;
  
  public AutoScalingBackendService() {
    this( 
        new PersistenceLaunchConfigurations( ),
        new PersistenceAutoScalingGroups( ),
        new PersistenceAutoScalingInstances( ),
        new PersistenceScalingPolicies( ),
        new ActivityManager( ),
        new PersistenceScalingActivities( ) );
  }

  protected AutoScalingBackendService( final LaunchConfigurations launchConfigurations,
                                       final AutoScalingGroups autoScalingGroups,
                                       final AutoScalingInstances autoScalingInstances,
                                       final ScalingPolicies scalingPolicies,
                                       final ActivityManager activityManager,
                                       final ScalingActivities scalingActivities ) {
    this.launchConfigurations = launchConfigurations;
    this.autoScalingGroups = autoScalingGroups;
    this.autoScalingInstances = autoScalingInstances;
    this.scalingPolicies = scalingPolicies;
    this.activityManager = activityManager;
    this.scalingActivities = scalingActivities;
  }

  public DescribeAutoScalingGroupsResponseType describeAutoScalingGroups( final DescribeAutoScalingGroupsType request ) throws EucalyptusCloudException {
    final DescribeAutoScalingGroupsResponseType reply = request.getReply( );

    //TODO: MaxRecords / NextToken support for DescribeAutoScalingGroups

    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.autoScalingGroupNames().remove( "verbose" );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    final Predicate<AutoScalingGroupMetadata> requestedAndAccessible = 
        AutoScalingMetadatas.filterPrivilegesByIdOrArn( AutoScalingGroupMetadata.class, request.autoScalingGroupNames() );

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

  public EnableMetricsCollectionResponseType enableMetricsCollection( final EnableMetricsCollectionType request ) throws EucalyptusCloudException {
    final EnableMetricsCollectionResponseType reply = request.getReply( );

    final Context ctx = Contexts.lookup( );
    try {
      final AccountFullName accountFullName = ctx.getUserFullName().asAccountFullName();
      final Callback<AutoScalingGroup> groupCallback = new Callback<AutoScalingGroup>() {
        @Override
        public void fire( final AutoScalingGroup autoScalingGroup ) {
          if ( RestrictedTypes.filterPrivileged().apply( autoScalingGroup ) ) {
            final Set<MetricCollectionType> metricsToEnable = EnumSet.allOf( MetricCollectionType.class );
            if ( request.getMetrics() != null && !request.getMetrics().getMember().isEmpty() ) {
              metricsToEnable.clear();
              Iterables.addAll( metricsToEnable, Iterables.transform(
                  request.getMetrics().getMember(),
                  Enums.valueOfFunction(MetricCollectionType.class) ) );
            }
            autoScalingGroup.getEnabledMetrics().addAll( metricsToEnable );
          }
        }
      };

      autoScalingGroups.update(
          accountFullName,
          request.getAutoScalingGroupName(),
          groupCallback );
    } catch ( AutoScalingMetadataNotFoundException e ) {
      throw new ValidationErrorException( "Auto scaling group not found: " + request.getAutoScalingGroupName() );
    } catch ( Exception e ) {
      handleException( e );
    }

    return reply;
  }

  public ResumeProcessesResponseType resumeProcesses( final ResumeProcessesType request ) throws EucalyptusCloudException {
    final ResumeProcessesResponseType reply = request.getReply( );

    final Context ctx = Contexts.lookup( );
    try {
      final AccountFullName accountFullName = ctx.getUserFullName().asAccountFullName();
      final Callback<AutoScalingGroup> groupCallback = new Callback<AutoScalingGroup>() {
        @Override
        public void fire( final AutoScalingGroup autoScalingGroup ) {
          if ( RestrictedTypes.filterPrivileged().apply( autoScalingGroup ) ) {
            final Set<ScalingProcessType> processesToResume = EnumSet.allOf( ScalingProcessType.class );
            if ( request.getScalingProcesses() != null && !request.getScalingProcesses().getMember().isEmpty() ) {
              processesToResume.clear();
              Iterables.addAll( processesToResume, Iterables.transform(
                  request.getScalingProcesses().getMember(),
                  Enums.valueOfFunction(ScalingProcessType.class) ) );
            }
            for ( final ScalingProcessType scalingProcessType : processesToResume ) {
              autoScalingGroup.getSuspendedProcesses().remove(
                    SuspendedProcess.createManual( scalingProcessType ) );
            }
          }
        }
      };

      autoScalingGroups.update(
          accountFullName,
          request.getAutoScalingGroupName(),
          groupCallback );
    } catch ( AutoScalingMetadataNotFoundException e ) {
      throw new ValidationErrorException( "Auto scaling group not found: " + request.getAutoScalingGroupName() );
    } catch ( Exception e ) {
      handleException( e );
    }

    return reply;
  }

  public DeleteLaunchConfigurationResponseType deleteLaunchConfiguration( final DeleteLaunchConfigurationType request ) throws EucalyptusCloudException {
    final DeleteLaunchConfigurationResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    try {
      final LaunchConfigurationMinimumView launchConfiguration = launchConfigurations.lookup(
          ctx.getUserFullName( ).asAccountFullName( ),
          request.getLaunchConfigurationName( ),
          TypeMappers.lookup( LaunchConfiguration.class, LaunchConfigurationMinimumView.class ) );
      if ( RestrictedTypes.filterPrivileged().apply( launchConfiguration ) ) {
        launchConfigurations.delete( launchConfiguration );
      } // else treat this as though the configuration does not exist
    } catch ( AutoScalingMetadataNotFoundException e ) {
      // so nothing to delete, move along      
    } catch ( Exception e ) {
      handleException( e );
    }    
    return reply;
  }

  public DescribePoliciesResponseType describePolicies(final DescribePoliciesType request) throws EucalyptusCloudException {
    final DescribePoliciesResponseType reply = request.getReply( );

    //TODO: MaxRecords / NextToken support for DescribePolicies

    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.policyNames().remove( "verbose" );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final Predicate<ScalingPolicy> requestedAndAccessible =
        Predicates.and( 
          AutoScalingMetadatas.filterPrivilegesByIdOrArn( ScalingPolicy.class, request.policyNames() ),
          AutoScalingResourceName.isResourceName().apply( request.getAutoScalingGroupName() ) ?
            AutoScalingMetadatas.filterByProperty(
                  AutoScalingResourceName.parse( request.getAutoScalingGroupName(), autoScalingGroup ).getUuid(),
                  ScalingPolicies.toGroupUuid() )  :
            AutoScalingMetadatas.filterByProperty( 
                request.getAutoScalingGroupName(), 
                ScalingPolicies.toGroupName() )
        );

      final List<ScalingPolicyType> results =
          reply.getDescribePoliciesResult().getScalingPolicies().getMember();
      results.addAll( scalingPolicies.list(
          ownerFullName,
          requestedAndAccessible,
          TypeMappers.lookup( ScalingPolicy.class, ScalingPolicyType.class ) ) );

      final List<String> scalingPolicyArns = Lists.transform( results, ScalingPolicyType.policyArn() );
      final Map<String,Collection<String>> policyArnToAlarmArns =
          activityManager.getAlarmsForPolicies( ctx.getUserFullName( ), scalingPolicyArns );

      for ( final ScalingPolicyType scalingPolicyType : results ) {
        final Collection<String> alarmArns = policyArnToAlarmArns.get( scalingPolicyType.getPolicyARN() );
        if ( alarmArns != null && !alarmArns.isEmpty() ) {
          scalingPolicyType.setAlarms( new Alarms( alarmArns ) );
        }
      }
    } catch ( Exception e ) {
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

  public CreateAutoScalingGroupResponseType createAutoScalingGroup( final CreateAutoScalingGroupType request ) throws EucalyptusCloudException {
    final CreateAutoScalingGroupResponseType reply = request.getReply( );

    final Context ctx = Contexts.lookup( );

    if ( request.getTags() != null ) {
      for ( final TagType tagType : request.getTags().getMember() ) {
        final String key = tagType.getKey();
        if ( com.google.common.base.Strings.isNullOrEmpty( key ) || key.trim().length() > 128 || isReserved( key ) ) {
          throw new ValidationErrorException( "Invalid key (max length 128, must not be empty, reserved prefixes "+reservedPrefixes+"): "+key );
        }
      }

      if ( request.getTags().getMember().size() >= MAX_TAGS_PER_RESOURCE ) {
        throw Exceptions.toUndeclared( new LimitExceededException("Tag limit exceeded") );
      }
    }

    final Supplier<AutoScalingGroup> allocator = new Supplier<AutoScalingGroup>( ) {
      @Override
      public AutoScalingGroup get( ) {
        try {
          final Integer minSize = Numbers.intValue( request.getMinSize() );
          final Integer maxSize = Numbers.intValue( request.getMaxSize() );
          final Integer desiredCapacity = Numbers.intValue( request.getDesiredCapacity() );

          if ( desiredCapacity != null && desiredCapacity < minSize ) {
            throw Exceptions.toUndeclared( new ValidationErrorException( "DesiredCapacity must not be less than MinSize" ) );
          }
          if ( desiredCapacity != null && desiredCapacity > maxSize ) {
            throw Exceptions.toUndeclared( new ValidationErrorException( "DesiredCapacity must not be greater than MaxSize" ) );
          }

          final List<String> referenceErrors = activityManager.validateReferences(
              ctx.getUserFullName(),
              request.availabilityZones(),
              request.loadBalancerNames()
          );
          verifyUnsupportedReferences( referenceErrors,
              request.getPlacementGroup(),
              request.getVpcZoneIdentifier() );

          if ( !referenceErrors.isEmpty() ) {
            throw Exceptions.toUndeclared( new ValidationErrorException( "Invalid parameters " + referenceErrors ) );
          }

          final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
          final AutoScalingGroups.PersistingBuilder builder = autoScalingGroups.create(
              ctx.getUserFullName(),
              request.getAutoScalingGroupName(),
              verifyOwnership( accountFullName, launchConfigurations.lookup( accountFullName, request.getLaunchConfigurationName(), Functions.<LaunchConfiguration>identity() ) ),
              minSize,
              maxSize )
              .withAvailabilityZones( request.availabilityZones() )
              .withDefaultCooldown( Numbers.intValue( request.getDefaultCooldown() ) )
              .withDesiredCapacity( desiredCapacity )
              .withHealthCheckGracePeriod( Numbers.intValue( request.getHealthCheckGracePeriod() ) )
              .withHealthCheckType(
                  request.getHealthCheckType()==null ? null : HealthCheckType.valueOf( request.getHealthCheckType() ) )
              .withLoadBalancerNames( request.loadBalancerNames() )
              .withTerminationPolicyTypes( request.terminationPolicies() == null ? null :
                  Collections2.filter( Collections2.transform( 
                    request.terminationPolicies(), Enums.valueOfFunction( TerminationPolicyType.class) ),
                    Predicates.not( Predicates.isNull() ) ) )
              .withTags( request.getTags()==null ?
                  null :
                  Iterables.transform( request.getTags().getMember(), TypeMappers.lookup( TagType.class, AutoScalingGroupTag.class ) ) );

          return builder.persist();
        } catch ( AutoScalingMetadataNotFoundException e ) {
          throw Exceptions.toUndeclared( new ValidationErrorException( "Launch configuration not found: " + request.getLaunchConfigurationName() ) );
        } catch ( IllegalArgumentException e ) {
          throw Exceptions.toUndeclared( new ValidationErrorException( "Invalid health check type: " + request.getHealthCheckType() ) );
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    };

    try {
      RestrictedTypes.allocateUnitlessResource( allocator );
    } catch ( Exception e ) {
      handleException( e, true );
    }

    return reply;
  }

  public DescribeScalingActivitiesResponseType describeScalingActivities( final DescribeScalingActivitiesType request ) throws EucalyptusCloudException {
    final DescribeScalingActivitiesResponseType reply = request.getReply( );

    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.activityIds().remove( "verbose" );
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

      reply.getDescribeScalingActivitiesResult().getActivities().getMember().addAll( scalingActivities );
    } catch ( AutoScalingMetadataNotFoundException e ) {
      throw new ValidationErrorException( "Auto scaling group not found: " + request.getAutoScalingGroupName() );
    } catch ( AutoScalingMetadataException e ) {
      handleException( e );
    }

    return reply;
  }

  public DescribeNotificationConfigurationsResponseType describeNotificationConfigurations(DescribeNotificationConfigurationsType request) throws EucalyptusCloudException {
    DescribeNotificationConfigurationsResponseType reply = request.getReply( );
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

  public DescribeTagsResponseType describeTags( final DescribeTagsType request ) throws EucalyptusCloudException {
    final DescribeTagsResponseType reply = request.getReply( );

    //TODO: MaxRecords / NextToken support for DescribeTags

    final Collection<Predicate<Tag>> tagFilters = Lists.newArrayList();
    for ( final Filter filter : request.filters() ) {
      final Function<Tag,String> extractor = tagFilterExtractors.get( filter.getName() );
      if ( extractor == null ) {
        throw new ValidationErrorException( "Filter type "+filter.getName()+" is not correct. Allowed Filter types are: auto-scaling-group key value propagate-at-launch" );
      }
      final Function<String,String> tagValueConverter = Objects.firstNonNull(
          tagValuePreProcessors.get( filter.getName() ),
          Functions.<String>identity() );
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
          Collections.<String,String>emptyMap() ) ) ) {
        if ( Permissions.isAuthorized(
            PolicySpec.VENDOR_AUTOSCALING,
            tag.getResourceType(),
            tag.getKey(),
            context.getAccount(),
            PolicySpec.describeAction( PolicySpec.VENDOR_AUTOSCALING, tag.getResourceType() ),
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

  public ExecutePolicyResponseType executePolicy(final ExecutePolicyType request) throws EucalyptusCloudException {
    final ExecutePolicyResponseType reply = request.getReply( );
    
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    try {
      final ScalingPolicyView scalingPolicy;
      try {
        scalingPolicy = scalingPolicies.lookup( 
          accountFullName,
          request.getAutoScalingGroupName(),
          request.getPolicyName(),
          TypeMappers.lookup( ScalingPolicy.class, ScalingPolicyView.class ));
      } catch ( AutoScalingMetadataNotFoundException e ) {
        throw new ValidationErrorException( "Scaling policy not found: " + request.getPolicyName() );
      }

      final Callback<AutoScalingGroup> updateCallback = new Callback<AutoScalingGroup>( ) {
        @Override
        public void fire( final AutoScalingGroup autoScalingGroup ) {
          final boolean isCloudWatch = Contexts.lookup( ).isPrivileged( );
          if ( isCloudWatch && !scalingProcessEnabled( ScalingProcessType.AlarmNotification, autoScalingGroup ) )  {
            logger.debug( "Ignoring policy execution due to alarm notification suspension" );
            return;
          }
          failIfScaling( activityManager, autoScalingGroup );
          final Integer desiredCapacity = scalingPolicy.getAdjustmentType().adjustCapacity(
              autoScalingGroup.getDesiredCapacity( ),
              scalingPolicy.getScalingAdjustment( ),
              Objects.firstNonNull( scalingPolicy.getMinAdjustmentStep( ), 0 ),
              Objects.firstNonNull( autoScalingGroup.getMinSize( ), 0 ),
              Objects.firstNonNull( autoScalingGroup.getMaxSize( ), Integer.MAX_VALUE )
          );
          setDesiredCapacityWithCooldown(
              autoScalingGroup,
              request.getHonorCooldown(),
              scalingPolicy.getCooldown(),
              desiredCapacity,
              String.format( isCloudWatch ?
                    "a CloudWatch alarm triggered policy %1$s changing the desired capacity from %2$d to %3$d" :
                    "a user request executed policy %1$s changing the desired capacity from %2$d to %3$d",
                  scalingPolicy.getDisplayName(),
                  autoScalingGroup.getDesiredCapacity(),
                  desiredCapacity ) );
        }
      };

      if ( RestrictedTypes.filterPrivileged( ).apply( scalingPolicy ) ) {
        autoScalingGroups.update(
            AccountFullName.getInstance( scalingPolicy.getOwnerAccountNumber( ) ),
            scalingPolicy.getAutoScalingGroupName( ),
            updateCallback );
      }
    } catch( Exception e ) {
      handleException( e );
    }    
    
    return reply;
  }

  public DeleteTagsResponseType deleteTags( final DeleteTagsType request ) throws EucalyptusCloudException {
    final DeleteTagsResponseType reply = request.getReply( );

    final Context context = Contexts.lookup();
    final OwnerFullName ownerFullName = context.getUserFullName().asAccountFullName();
    final List<TagType> tagTypes = Objects.firstNonNull( request.getTags().getMember(), Collections.<TagType>emptyList() );

    for ( final TagType tagType : tagTypes ) {
      final String key = tagType.getKey();
      if ( com.google.common.base.Strings.isNullOrEmpty( key ) || key.trim().length() > 128 || isReserved( key ) ) {
        throw new ValidationErrorException( "Invalid key (max length 128, must not be empty, reserved prefixes "+reservedPrefixes+"): "+key );
      }
    }

    if ( tagTypes.size() > 0 ) {
      final Predicate<Void> delete = new Predicate<Void>(){
        @Override
        public boolean apply( final Void v ) {
          for ( final TagType tagType : tagTypes ) {
            try {
              final TagSupport tagSupport = TagSupport.fromResourceType( tagType.getResourceType() );
              final AutoScalingMetadata resource =
                  tagSupport.lookup( ownerFullName, tagType.getResourceId() );
              final Tag example = tagSupport.example( resource, ownerFullName, tagType.getKey(), tagType.getValue() );
              if ( example != null && Permissions.isAuthorized(
                  PolicySpec.VENDOR_AUTOSCALING,
                  PolicySpec.AUTOSCALING_RESOURCE_TAG,
                  example.getResourceType() + ":" + example.getResourceId() + ":" + example.getKey(),
                  context.getAccount(),
                  PolicySpec.AUTOSCALING_DELETETAGS,
                  context.getAuthContext() ) ) {
                Tags.delete( example );
              }
            } catch ( AutoScalingMetadataNotFoundException e ) {
              logger.debug( e, e );
            } catch ( TransactionException e ) {
              throw Exceptions.toUndeclared(e);
            }
          }
          return true;
        }
      };

      try {
        Entities.asTransaction( Tag.class, delete ).apply( null );
      } catch ( Exception e ) {
        handleException( e );
      }
    }

    return reply;
  }

  public PutScalingPolicyResponseType putScalingPolicy(final PutScalingPolicyType request) throws EucalyptusCloudException {
    final PutScalingPolicyResponseType reply = request.getReply( );

    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName();
    try {
      // Try update
      final ScalingPolicy scalingPolicy = scalingPolicies.update(
          accountFullName,
          request.getAutoScalingGroupName(),
          request.getPolicyName(), new Callback<ScalingPolicy>() {
        @Override
        public void fire( final ScalingPolicy scalingPolicy ) {
          if ( RestrictedTypes.filterPrivileged().apply( scalingPolicy ) ) {
            if ( request.getAdjustmentType() != null )
              scalingPolicy.setAdjustmentType( 
                  Enums.valueOfFunction( AdjustmentType.class ).apply( request.getAdjustmentType() ) );
            if ( request.getScalingAdjustment() != null ) {
              if ( AdjustmentType.ExactCapacity == scalingPolicy.getAdjustmentType() &&
                  request.getScalingAdjustment() < 0 ) {
                throw Exceptions.toUndeclared( new ValidationErrorException( "ScalingAdjustment cannot be negative with the specified adjustment type." ) );
              }
              scalingPolicy.setScalingAdjustment( request.getScalingAdjustment() );
            }
            if ( request.getCooldown() != null )
              scalingPolicy.setCooldown( request.getCooldown() );
            if ( request.getMinAdjustmentStep() != null ) {
              if ( AdjustmentType.PercentChangeInCapacity != scalingPolicy.getAdjustmentType() ) {
                throw Exceptions.toUndeclared( new ValidationErrorException( "MinAdjustmentStep is not supported by the specified adjustment type." ) );
              }
              scalingPolicy.setMinAdjustmentStep( request.getMinAdjustmentStep() );
            }
          }
        }
      } );
      reply.getPutScalingPolicyResult().setPolicyARN( scalingPolicy.getArn() );
    } catch ( AutoScalingMetadataNotFoundException e ) {
      // Not found, create
      final Supplier<ScalingPolicy> allocator = new Supplier<ScalingPolicy>( ) {
        @Override
        public ScalingPolicy get( ) {
          try {
            final AdjustmentType adjustmentType =
                Enums.valueOfFunction( AdjustmentType.class ).apply( request.getAdjustmentType() );

            if ( request.getMinAdjustmentStep() != null &&
                AdjustmentType.PercentChangeInCapacity != adjustmentType ) {
              throw Exceptions.toUndeclared( new ValidationErrorException( "MinAdjustmentStep is not supported by the specified adjustment type." ) );
            }

            if ( AdjustmentType.ExactCapacity == adjustmentType &&
                request.getScalingAdjustment() < 0 ) {
              throw Exceptions.toUndeclared( new ValidationErrorException( "ScalingAdjustment cannot be negative with the specified adjustment type." ) );
            }

            final ScalingPolicies.PersistingBuilder builder = scalingPolicies.create(
                ctx.getUserFullName( ),
                verifyOwnership( accountFullName, autoScalingGroups.lookup( accountFullName, request.getAutoScalingGroupName(), Functions.<AutoScalingGroup>identity() ) ),
                request.getPolicyName(),
                adjustmentType,
                request.getScalingAdjustment() )
                .withCooldown( request.getCooldown() )
                .withMinAdjustmentStep( request.getMinAdjustmentStep() );

            return builder.persist();
          } catch ( AutoScalingMetadataNotFoundException e ) {
            throw Exceptions.toUndeclared( new ValidationErrorException( "Auto scaling group not found: " + request.getAutoScalingGroupName() ) );
          } catch ( IllegalArgumentException e ) {
            throw Exceptions.toUndeclared( new ValidationErrorException( "Invalid adjustment type: " + request.getAdjustmentType() ) );
          } catch ( Exception ex ) {
            throw new RuntimeException( ex );
          }
        }
      };

      try {
        final ScalingPolicy scalingPolicy = RestrictedTypes.allocateUnitlessResource( allocator );
        reply.getPutScalingPolicyResult().setPolicyARN( scalingPolicy.getArn() );
      } catch ( Exception exception ) {
        handleException( exception, true );
      }

    } catch ( Exception e ) {
      handleException( e );
    }

    return reply;
  }

  public PutNotificationConfigurationResponseType putNotificationConfiguration(PutNotificationConfigurationType request) throws EucalyptusCloudException {
    PutNotificationConfigurationResponseType reply = request.getReply( );
    return reply;
  }

  public DeletePolicyResponseType deletePolicy(final DeletePolicyType request) throws EucalyptusCloudException {
    final DeletePolicyResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    try {
      final ScalingPolicyView scalingPolicy = scalingPolicies.lookup(
          ctx.getUserFullName( ).asAccountFullName( ),
          request.getAutoScalingGroupName(),
          request.getPolicyName( ),
          TypeMappers.lookup( ScalingPolicy.class, ScalingPolicyView.class ) );
      if ( RestrictedTypes.filterPrivileged().apply( scalingPolicy ) ) {
        scalingPolicies.delete( scalingPolicy );
      } // else treat this as though the configuration does not exist
    } catch ( AutoScalingMetadataNotFoundException e ) {
      // so nothing to delete, move along      
    } catch ( Exception e ) {
      handleException( e );
    }
    return reply;
  }

  public DeleteNotificationConfigurationResponseType deleteNotificationConfiguration(DeleteNotificationConfigurationType request) throws EucalyptusCloudException {
    DeleteNotificationConfigurationResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteScheduledActionResponseType deleteScheduledAction(DeleteScheduledActionType request) throws EucalyptusCloudException {
    DeleteScheduledActionResponseType reply = request.getReply( );
    return reply;
  }

  public SetInstanceHealthResponseType setInstanceHealth(final SetInstanceHealthType request) throws EucalyptusCloudException {
    final SetInstanceHealthResponseType reply = request.getReply( );

    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final Callback<AutoScalingInstance> instanceUpdateCallback = new Callback<AutoScalingInstance>() {
        @Override
        public void fire( final AutoScalingInstance instance ) {
          if ( RestrictedTypes.filterPrivileged().apply( instance ) ) {
            if ( !Objects.firstNonNull( request.getShouldRespectGracePeriod(), Boolean.FALSE ) ||
                instance.healthStatusGracePeriodExpired() ) {
              instance.setHealthStatus( Enums.valueOfFunction( HealthStatus.class ).apply( request.getHealthStatus( ) ) );
            }
          } else {
            throw Exceptions.toUndeclared( new AutoScalingMetadataNotFoundException("Instance not found") );
          }
        }
      };      
      autoScalingInstances.update( ownerFullName, request.getInstanceId(), instanceUpdateCallback );
    } catch ( AutoScalingMetadataNotFoundException e ) {
      throw new ValidationErrorException( "Auto scaling instance not found: " + request.getInstanceId( ) );
    } catch ( Exception e ) {
      handleException( e );
    }
    
    return reply;
  }

  public DescribeAutoScalingNotificationTypesResponseType describeAutoScalingNotificationTypes(DescribeAutoScalingNotificationTypesType request) throws EucalyptusCloudException {
    DescribeAutoScalingNotificationTypesResponseType reply = request.getReply( );
    return reply;
  }

  public CreateOrUpdateTagsResponseType createOrUpdateTags( final CreateOrUpdateTagsType request ) throws EucalyptusCloudException {
    final CreateOrUpdateTagsResponseType reply = request.getReply( );

    final Context context = Contexts.lookup();
    final UserFullName ownerFullName = context.getUserFullName();
    final AccountFullName accountFullName = ownerFullName.asAccountFullName();

    for ( final TagType tagType : request.getTags().getMember() ) {
      final String key = tagType.getKey();
      final String value = com.google.common.base.Strings.nullToEmpty( tagType.getValue() ).trim();

      if ( com.google.common.base.Strings.isNullOrEmpty( key ) || key.trim().length() > 128 || isReserved( key ) ) {
        throw new ValidationErrorException( "Invalid key (max length 128, must not be empty, reserved prefixes "+reservedPrefixes+"): "+key );
      }
      if ( value.length() > 256 || isReserved( key ) ) {
        throw new ValidationErrorException( "Invalid value (max length 256, reserved prefixes "+reservedPrefixes+"): "+value );
      }
    }

    if ( request.getTags().getMember().size() > 0 ) {
      final Predicate<Void> creator = new Predicate<Void>(){
        @Override
        public boolean apply( final Void v ) {
          try {
            for ( final TagType tagType : request.getTags().getMember() ) {
              final TagSupport tagSupport = TagSupport.fromResourceType( tagType.getResourceType() );
              AutoScalingMetadata resource = null;
              try {
                resource = tagSupport.lookup( accountFullName, tagType.getResourceId() );
              } catch ( NoSuchElementException e ) {
                Logs.extreme().info( e, e );
              }

              if ( resource == null || !RestrictedTypes.filterPrivileged().apply( resource ) ) {
                throw Exceptions.toUndeclared( new ValidationErrorException( "Resource not found " + tagType.getResourceId() ) );
              }

              final String key = com.google.common.base.Strings.nullToEmpty( tagType.getKey() ).trim();
              final String value = com.google.common.base.Strings.nullToEmpty( tagType.getValue() ).trim();
              final Boolean propagateAtLaunch = Objects.firstNonNull( tagType.getPropagateAtLaunch(), Boolean.FALSE );
              tagSupport.createOrUpdate( resource, ownerFullName, key, value, propagateAtLaunch );

              final Tag example = tagSupport.example( resource, accountFullName, null, null );
              if ( Entities.count( example ) > MAX_TAGS_PER_RESOURCE ) {
                throw Exceptions.toUndeclared( new LimitExceededException("Tag limit exceeded for resource '"+resource.getDisplayName()+"'") );
              }
            }
            return true;
          } catch ( Exception e ) {
            throw Exceptions.toUndeclared( e );
          }
        }
      };

      try {
        Entities.asTransaction( Tag.class, creator ).apply( null );
      } catch ( Exception e ) {
        handleException( e, true );
      }
    }

    return reply;
  }

  public SuspendProcessesResponseType suspendProcesses( final SuspendProcessesType request ) throws EucalyptusCloudException {
    final SuspendProcessesResponseType reply = request.getReply( );

    final Context ctx = Contexts.lookup( );
    try {
      final AccountFullName accountFullName = ctx.getUserFullName().asAccountFullName();
      final Callback<AutoScalingGroup> groupCallback = new Callback<AutoScalingGroup>() {
        @Override
        public void fire( final AutoScalingGroup autoScalingGroup ) {
          if ( RestrictedTypes.filterPrivileged().apply( autoScalingGroup ) ) {
            final boolean isAdminSuspension =
                ctx.isAdministrator( ) &&
                !autoScalingGroup.getOwnerAccountNumber().equals( accountFullName.getAccountNumber() );

            final Set<ScalingProcessType> processesToSuspend = EnumSet.allOf( ScalingProcessType.class );
            if ( request.getScalingProcesses() != null && !request.getScalingProcesses().getMember().isEmpty() ) {
              processesToSuspend.clear();
              Iterables.addAll( processesToSuspend, Iterables.transform(
                  request.getScalingProcesses().getMember(),
                  Enums.valueOfFunction(ScalingProcessType.class) ) );
            }
            for ( final ScalingProcessType scalingProcessType : processesToSuspend ) {
              if ( scalingProcessType.apply( autoScalingGroup ) ) {
                autoScalingGroup.getSuspendedProcesses().add(
                    isAdminSuspension ?
                        SuspendedProcess.createAdministrative( scalingProcessType ) :
                        SuspendedProcess.createManual( scalingProcessType ) );
              }
            }
          }
        }
      };

      autoScalingGroups.update(
          accountFullName,
          request.getAutoScalingGroupName(),
          groupCallback );
    } catch ( AutoScalingMetadataNotFoundException e ) {
      throw new ValidationErrorException( "Auto scaling group not found: " + request.getAutoScalingGroupName() );
    } catch ( Exception e ) {
      handleException( e );
    }

    return reply;
  }

  public DescribeAutoScalingInstancesResponseType describeAutoScalingInstances( final DescribeAutoScalingInstancesType request ) throws EucalyptusCloudException {
    final DescribeAutoScalingInstancesResponseType reply = request.getReply( );

    //TODO: MaxRecords / NextToken support for DescribeAutoScalingInstances

    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.instanceIds().remove( "verbose" );
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

  public CreateLaunchConfigurationResponseType createLaunchConfiguration( final CreateLaunchConfigurationType request ) throws EucalyptusCloudException {
    final CreateLaunchConfigurationResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final Supplier<LaunchConfiguration> allocator = new Supplier<LaunchConfiguration>( ) {
      @Override
      public LaunchConfiguration get( ) {
        try {
          final LaunchConfigurations.PersistingBuilder builder = launchConfigurations.create(
              ctx.getUserFullName(),
              request.getLaunchConfigurationName(),
              request.getImageId(),
              request.getInstanceType() )
            .withKernelId( request.getKernelId() )
            .withRamdiskId( request.getRamdiskId() )
            .withKeyName( request.getKeyName() )
            .withUserData( request.getUserData() )
            .withInstanceMonitoring( request.getInstanceMonitoring() != null ? request.getInstanceMonitoring().getEnabled() : null )
            .withInstanceProfile( request.getIamInstanceProfile() )
            .withSecurityGroups( request.getSecurityGroups() != null ? request.getSecurityGroups().getMember() : null );          
            
          if ( request.getBlockDeviceMappings() != null ) {
            for ( final BlockDeviceMappingType blockDeviceMappingType : request.getBlockDeviceMappings().getMember() ) {
              builder.withBlockDeviceMapping( 
                  blockDeviceMappingType.getDeviceName(),
                  blockDeviceMappingType.getVirtualName(),
                  blockDeviceMappingType.getEbs() != null ? blockDeviceMappingType.getEbs().getSnapshotId() : null,
                  blockDeviceMappingType.getEbs() != null ? Numbers.intValue( blockDeviceMappingType.getEbs().getVolumeSize() ) : null ); 
            }
          }

          final List<String> referenceErrors = activityManager.validateReferences(
              ctx.getUserFullName(),
              Iterables.filter( Lists.newArrayList( request.getImageId(), request.getKernelId(), request.getRamdiskId() ), Predicates.notNull() ),
              request.getInstanceType(),
              request.getKeyName(),
              request.getSecurityGroups() == null ? Collections.<String>emptyList() : request.getSecurityGroups().getMember(),
              request.getIamInstanceProfile()
          );

          if ( !referenceErrors.isEmpty() ) {
            throw Exceptions.toUndeclared( new ValidationErrorException( "Invalid parameters " + referenceErrors ) );
          }

          return builder.persist();
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    };

    try {
      RestrictedTypes.allocateUnitlessResource( allocator );
    } catch ( Exception e ) {
      handleException( e, true );
    }

    return reply;
  }

  public DeleteAutoScalingGroupResponseType deleteAutoScalingGroup( final DeleteAutoScalingGroupType request ) throws EucalyptusCloudException {
    final DeleteAutoScalingGroupResponseType reply = request.getReply( );
    
    final Context ctx = Contexts.lookup( );
    try {
      final AutoScalingGroupCoreView group = autoScalingGroups.lookup(
          ctx.getUserFullName( ).asAccountFullName( ),
          request.getAutoScalingGroupName(),
          TypeMappers.lookup( AutoScalingGroup.class, AutoScalingGroupCoreView.class ) );
      if ( RestrictedTypes.filterPrivileged().apply( group ) ) {
        // Terminate instances first if requested (but don't wait for success ...)
        if ( Objects.firstNonNull( request.getForceDelete(), Boolean.FALSE ) ) {
          final List<String> instanceIds = autoScalingInstances.listByGroup(
              group,
              Predicates.alwaysTrue(),
              AutoScalingInstances.instanceId() );
          if ( !instanceIds.isEmpty() ) {
            final List<ScalingActivity> activities =
                activityManager.terminateInstances( group, instanceIds );
            if ( activities==null || activities.isEmpty() ) {
              throw new ScalingActivityInProgressException("Scaling activity in progress");
            }
            autoScalingInstances.deleteByGroup( group );
          }
        } else {
          failIfScaling( activityManager, group );
        }
        autoScalingGroups.delete( group );
      } // else treat this as though the group does not exist
    } catch ( AutoScalingMetadataNotFoundException e ) {
      // so nothing to delete, move along      
    } catch ( Exception e ) {
      handleException( e );
    }
    
    return reply;
  }

  public DisableMetricsCollectionResponseType disableMetricsCollection( final DisableMetricsCollectionType request ) throws EucalyptusCloudException {
    final DisableMetricsCollectionResponseType reply = request.getReply( );

    final Context ctx = Contexts.lookup( );
    try {
      final AccountFullName accountFullName = ctx.getUserFullName().asAccountFullName();
      final Callback<AutoScalingGroup> groupCallback = new Callback<AutoScalingGroup>() {
        @Override
        public void fire( final AutoScalingGroup autoScalingGroup ) {
          if ( RestrictedTypes.filterPrivileged().apply( autoScalingGroup ) ) {
            final Set<MetricCollectionType> metricsToDisable = EnumSet.allOf( MetricCollectionType.class );
            if ( request.getMetrics() != null && !request.getMetrics().getMember().isEmpty() ) {
              metricsToDisable.clear();
              Iterables.addAll( metricsToDisable, Iterables.transform(
                  request.getMetrics().getMember(),
                  Enums.valueOfFunction(MetricCollectionType.class) ) );
            }
            autoScalingGroup.getEnabledMetrics().removeAll( metricsToDisable );
          }
        }
      };

      autoScalingGroups.update(
          accountFullName,
          request.getAutoScalingGroupName(),
          groupCallback );
    } catch ( AutoScalingMetadataNotFoundException e ) {
      throw new ValidationErrorException( "Auto scaling group not found: " + request.getAutoScalingGroupName() );
    } catch ( Exception e ) {
      handleException( e );
    }

    return reply;
  }

  public UpdateAutoScalingGroupResponseType updateAutoScalingGroup( final UpdateAutoScalingGroupType request ) throws EucalyptusCloudException {
    final UpdateAutoScalingGroupResponseType reply = request.getReply( );

    final Context ctx = Contexts.lookup( );
    try {
      final AccountFullName accountFullName = ctx.getUserFullName().asAccountFullName();
      final Callback<AutoScalingGroup> groupCallback = new Callback<AutoScalingGroup>() {
        @Override
        public void fire( final AutoScalingGroup autoScalingGroup ) {
          if ( RestrictedTypes.filterPrivileged().apply( autoScalingGroup ) ) {
            if ( request.availabilityZones() != null && !request.availabilityZones().isEmpty() )
              autoScalingGroup.updateAvailabilityZones( Lists.newArrayList( Sets.newLinkedHashSet( request.availabilityZones() ) ) );
            if ( request.getDefaultCooldown() != null )
              autoScalingGroup.setDefaultCooldown( Numbers.intValue( request.getDefaultCooldown( ) ) );
            if ( request.getHealthCheckGracePeriod( ) != null )
              autoScalingGroup.setHealthCheckGracePeriod( Numbers.intValue( request.getHealthCheckGracePeriod( ) ) );
            if ( request.getHealthCheckType( ) != null )
              autoScalingGroup.setHealthCheckType( Enums.valueOfFunction( HealthCheckType.class ).apply( request.getHealthCheckType( ) ) );
            if ( request.getLaunchConfigurationName( ) != null )
              try {
                autoScalingGroup.setLaunchConfiguration( verifyOwnership(
                    accountFullName,
                    launchConfigurations.lookup( accountFullName, request.getLaunchConfigurationName(), Functions.<LaunchConfiguration>identity() ) ) );
              } catch ( AutoScalingMetadataNotFoundException e ) {
                throw Exceptions.toUndeclared( new ValidationErrorException( "Launch configuration not found: " + request.getLaunchConfigurationName() ) );
              } catch ( AutoScalingMetadataException e ) {
                throw Exceptions.toUndeclared( e );
              }
            if ( request.getMaxSize( ) != null )
              autoScalingGroup.setMaxSize( Numbers.intValue( request.getMaxSize() ) );
            if ( request.getMinSize( ) != null )
              autoScalingGroup.setMinSize( Numbers.intValue( request.getMinSize( ) ) );
            if ( request.terminationPolicies() != null && !request.terminationPolicies().isEmpty() )
              autoScalingGroup.setTerminationPolicies( Lists.newArrayList(
                  Sets.newLinkedHashSet( Iterables.filter(
                      Iterables.transform( request.terminationPolicies( ), Enums.valueOfFunction( TerminationPolicyType.class ) ),
                      Predicates.not( Predicates.isNull( ) ) ) ) ) );
            if ( request.getDesiredCapacity() != null ) {
              Integer updatedDesiredCapacity = Numbers.intValue( request.getDesiredCapacity( ) );
              autoScalingGroup.updateDesiredCapacity(
                  updatedDesiredCapacity,
                  String.format("a user request update of AutoScalingGroup constraints to min: %1$d, max: %2$d, desired: %4$d changing the desired capacity from %3$d to %4$d",
                      autoScalingGroup.getMinSize(),
                      autoScalingGroup.getMaxSize(),
                      autoScalingGroup.getDesiredCapacity(),
                      updatedDesiredCapacity ) );
            }

            if ( autoScalingGroup.getDesiredCapacity() < autoScalingGroup.getMinSize() ) {
              throw Exceptions.toUndeclared( new ValidationErrorException( "DesiredCapacity must not be less than MinSize" ) );
            }

            if ( autoScalingGroup.getDesiredCapacity() > autoScalingGroup.getMaxSize() ) {
              throw Exceptions.toUndeclared( new ValidationErrorException( "DesiredCapacity must not be greater than MaxSize" ) );
            }

            final List<String> referenceErrors = activityManager.validateReferences(
                autoScalingGroup.getOwner(),
                autoScalingGroup.getAvailabilityZones(),
                Collections.<String>emptyList() // load balancer names cannot be updated
            );
            verifyUnsupportedReferences( referenceErrors,
                request.getPlacementGroup(),
                request.getVpcZoneIdentifier() );

            if ( !referenceErrors.isEmpty() ) {
              throw Exceptions.toUndeclared( new ValidationErrorException( "Invalid parameters " + referenceErrors ) );
            }
          }
        }
      };

      autoScalingGroups.update(
          accountFullName,
          request.getAutoScalingGroupName(),
          groupCallback );
    } catch ( AutoScalingMetadataNotFoundException e ) {
      throw new ValidationErrorException( "Auto scaling group not found: " + request.getAutoScalingGroupName() );
    } catch ( Exception e ) {
      handleException( e );
    }
    
    return reply;
  }

  public DescribeLaunchConfigurationsResponseType describeLaunchConfigurations(DescribeLaunchConfigurationsType request) throws EucalyptusCloudException {
    final DescribeLaunchConfigurationsResponseType reply = request.getReply( );

    //TODO: MaxRecords / NextToken support for DescribeLaunchConfigurations
    
    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.launchConfigurationNames().remove( "verbose" );  
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll ?
        null : 
        ctx.getUserFullName( ).asAccountFullName( );

    final Predicate<LaunchConfigurationMetadata> requestedAndAccessible =
        AutoScalingMetadatas.filterPrivilegesByIdOrArn( LaunchConfigurationMetadata.class, request.launchConfigurationNames() );

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

  public DescribeAdjustmentTypesResponseType describeAdjustmentTypes(final DescribeAdjustmentTypesType request) throws EucalyptusCloudException {
    final DescribeAdjustmentTypesResponseType reply = request.getReply( );

    reply.getDescribeAdjustmentTypesResult().setAdjustmentTypes( new AdjustmentTypes(
        Collections2.transform(
            Collections2.filter( EnumSet.allOf( AdjustmentType.class ), RestrictedTypes.filterPrivilegedWithoutOwner() ),
            Strings.toStringFunction() ) ) );

    return reply;
  }

  public DescribeScheduledActionsResponseType describeScheduledActions(DescribeScheduledActionsType request) throws EucalyptusCloudException {
    DescribeScheduledActionsResponseType reply = request.getReply( );
    return reply;
  }

  public PutScheduledUpdateGroupActionResponseType putScheduledUpdateGroupAction(PutScheduledUpdateGroupActionType request) throws EucalyptusCloudException {
    PutScheduledUpdateGroupActionResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeMetricCollectionTypesResponseType describeMetricCollectionTypes( final DescribeMetricCollectionTypesType request ) throws EucalyptusCloudException {
    final DescribeMetricCollectionTypesResponseType reply = request.getReply( );

    reply.getDescribeMetricCollectionTypesResult().setMetrics( new MetricCollectionTypes(
        Collections2.transform(
            Collections2.filter( EnumSet.allOf( MetricCollectionType.class ), RestrictedTypes.filterPrivilegedWithoutOwner() ),
            Strings.toStringFunction() ) ) );

    return reply;
  }

  public SetDesiredCapacityResponseType setDesiredCapacity( final SetDesiredCapacityType request ) throws EucalyptusCloudException {
    final SetDesiredCapacityResponseType reply = request.getReply( );

    final Context ctx = Contexts.lookup( );
    try {
      final Callback<AutoScalingGroup> groupCallback = new Callback<AutoScalingGroup>() {
        @Override
        public void fire( final AutoScalingGroup autoScalingGroup ) {
          if ( RestrictedTypes.filterPrivileged().apply( autoScalingGroup ) ) {
            failIfScaling( activityManager, autoScalingGroup );
            final Integer desiredCapacity = Numbers.intValue( request.getDesiredCapacity( ) );

            if ( desiredCapacity < autoScalingGroup.getMinSize( ) ) {
              throw Exceptions.toUndeclared( new ValidationErrorException(
                  String.format( "New SetDesiredCapacity value %d is below min value %d for the AutoScalingGroup.",
                      desiredCapacity,
                      autoScalingGroup.getMinSize( ) ) ) );
            }

            if ( desiredCapacity > autoScalingGroup.getMaxSize( ) ) {
              throw Exceptions.toUndeclared( new ValidationErrorException(
                  String.format( "New SetDesiredCapacity value %d is above max value %d for the AutoScalingGroup.",
                      desiredCapacity,
                      autoScalingGroup.getMaxSize( ) ) ) );
            }

            setDesiredCapacityWithCooldown(
                autoScalingGroup, 
                request.getHonorCooldown(), 
                null,
                desiredCapacity,
                String.format( "a user request explicitly set group desired capacity changing the desired capacity from %1$d to %2$d",
                    autoScalingGroup.getDesiredCapacity(),
                    desiredCapacity ) );
          } 
        }
      };

      autoScalingGroups.update(
          ctx.getUserFullName().asAccountFullName(),
          request.getAutoScalingGroupName(),
          groupCallback);
    } catch ( AutoScalingMetadataNotFoundException e ) {
      throw new ValidationErrorException( "Auto scaling group not found: " + request.getAutoScalingGroupName() );
    } catch ( Exception e ) {
      handleException( e );
    }    
    
    return reply;
  }

  public TerminateInstanceInAutoScalingGroupResponseType terminateInstanceInAutoScalingGroup( final TerminateInstanceInAutoScalingGroupType request ) throws EucalyptusCloudException {
    final TerminateInstanceInAutoScalingGroupResponseType reply = request.getReply( );
    
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final AutoScalingInstanceGroupView instance = autoScalingInstances.lookup(
          ownerFullName,
          request.getInstanceId(),
          TypeMappers.lookup( AutoScalingInstance.class, AutoScalingInstanceGroupView.class ));
      if ( !RestrictedTypes.filterPrivileged().apply( instance ) ) {
        throw new AutoScalingMetadataNotFoundException("Instance not found");
      }
      
      final List<ScalingActivity> activities = activityManager.terminateInstances(
          instance.getAutoScalingGroup(),
          Collections.singletonList( instance.getInstanceId() ) );
      if ( activities == null || activities.isEmpty() ) {
        throw new ScalingActivityInProgressException("Scaling activity in progress");
      }
      reply.getTerminateInstanceInAutoScalingGroupResult().setActivity( 
        TypeMappers.transform( activities.get( 0 ), Activity.class )  
      );

      final String groupArn = instance.getAutoScalingGroup().getArn();
      final Callback<AutoScalingGroup> groupCallback = new Callback<AutoScalingGroup>() {
        @Override
        public void fire( final AutoScalingGroup autoScalingGroup ) {
          if ( Objects.firstNonNull( request.getShouldDecrementDesiredCapacity(), Boolean.FALSE ) ) {
            autoScalingGroup.setDesiredCapacity( autoScalingGroup.getDesiredCapacity() - 1 );
          } else {
            autoScalingGroup.setScalingRequired( true );
          }
        }
      };

      autoScalingGroups.update(
          ctx.getUserFullName().asAccountFullName(),
          groupArn,
          groupCallback);
    } catch ( AutoScalingMetadataNotFoundException e ) {
      throw new ValidationErrorException( "Auto scaling instance not found: " + request.getInstanceId( ) );
    } catch ( Exception e ) {
      handleException( e );
    }
    
    return reply;
  }

  private static void failIfScaling( final ActivityManager activityManager,
                                     final AutoScalingGroupMetadata group ) {
    if ( activityManager.scalingInProgress( group ) ) {
      throw Exceptions.toUndeclared( new ScalingActivityInProgressException("Scaling activity in progress") );      
    }
  }

  private static boolean scalingProcessEnabled( final ScalingProcessType type, final AutoScalingGroup group ) {
    return !AutoScalingConfiguration.getSuspendedProcesses().contains( type ) && type.apply( group );
  }

  private static void setDesiredCapacityWithCooldown( final AutoScalingGroup autoScalingGroup,
                                                      final Boolean honorCooldown,
                                                      final Integer cooldown,
                                                      final int capacity,
                                                      final String reason ) {
    final long cooldownMs = TimeUnit.SECONDS.toMillis( Objects.firstNonNull( cooldown, autoScalingGroup.getDefaultCooldown() ) );
    if ( !Objects.firstNonNull( honorCooldown, Boolean.FALSE ) ||
        ( System.currentTimeMillis() - autoScalingGroup.getCapacityTimestamp().getTime() ) > cooldownMs ) {
      autoScalingGroup.updateDesiredCapacity( capacity, reason );
      autoScalingGroup.setCapacityTimestamp( new Date() );
    } else {
      throw Exceptions.toUndeclared( new InternalFailureException("Group is in cooldown") );
    }
  }

  private static void verifyUnsupportedReferences( final List<String> referenceErrors,
                                                   final String placementGroup,
                                                   final String vpcZoneIdentifier ) {
    if ( !com.google.common.base.Strings.isNullOrEmpty( placementGroup ) ) {
      referenceErrors.add( "Invalid placement group: " + placementGroup );
    }

    if ( !com.google.common.base.Strings.isNullOrEmpty( vpcZoneIdentifier ) ) {
      referenceErrors.add( "Invalid VPC zone identifier: " + vpcZoneIdentifier );
    }
  }

  private static <AOP extends AbstractOwnedPersistent> AOP verifyOwnership(
      final OwnerFullName ownerFullName,
      final AOP aop
  ) throws AutoScalingMetadataNotFoundException {
    if ( !AutoScalingMetadatas.filterByOwner( ownerFullName ).apply( aop ) ) {
      throw new AutoScalingMetadataNotFoundException( "Not found: " + aop.getDisplayName( ) );
    }
    return aop;
  }

  private static boolean isReserved( final String text ) {
    return Iterables.any( reservedPrefixes, Strings.isPrefixOf( text ) );
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
    
    final AuthQuotaException quotaCause = Exceptions.findCause( e, AuthQuotaException.class );
    if ( quotaCause != null ) {
      throw new LimitExceededException( "Request would exceed quota for type: " + quotaCause.getType() );
    }
    
    final ConstraintViolationException constraintViolationException = 
        Exceptions.findCause( e, ConstraintViolationException.class );
    if ( constraintViolationException != null ) {
      throw isCreate ? 
          new AlreadyExistsException( "Resource already exists" ):
          new ResourceInUseException( "Resource in use" );
    }

    final InvalidResourceNameException invalidResourceNameException =
        Exceptions.findCause( e, InvalidResourceNameException.class );
    if ( invalidResourceNameException != null ) {
      throw new ValidationErrorException( invalidResourceNameException.getMessage() );
    }

    logger.error( e, e );

    final InternalFailureException exception = new InternalFailureException( String.valueOf(e.getMessage()) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );      
    }
    throw exception;
  }
}
