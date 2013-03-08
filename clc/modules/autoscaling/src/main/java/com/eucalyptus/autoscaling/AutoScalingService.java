/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.autoscaling;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingInstanceMetadata;
import static com.eucalyptus.autoscaling.common.AutoScalingResourceName.InvalidResourceNameException;
import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingGroupMetadata;
import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.LaunchConfigurationMetadata;
import static com.eucalyptus.autoscaling.common.AutoScalingResourceName.Type.autoScalingGroup;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
import com.eucalyptus.autoscaling.activities.ScalingActivity;
import com.eucalyptus.autoscaling.common.Activity;
import com.eucalyptus.autoscaling.common.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.AutoScalingInstanceDetails;
import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
import com.eucalyptus.autoscaling.common.BlockDeviceMappingType;
import com.eucalyptus.autoscaling.common.CreateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.CreateAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.CreateLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.common.CreateLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.CreateOrUpdateTagsResponseType;
import com.eucalyptus.autoscaling.common.CreateOrUpdateTagsType;
import com.eucalyptus.autoscaling.common.DeleteAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.DeleteAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.DeleteLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.common.DeleteLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.DeleteNotificationConfigurationResponseType;
import com.eucalyptus.autoscaling.common.DeleteNotificationConfigurationType;
import com.eucalyptus.autoscaling.common.DeletePolicyResponseType;
import com.eucalyptus.autoscaling.common.DeletePolicyType;
import com.eucalyptus.autoscaling.common.DeleteScheduledActionResponseType;
import com.eucalyptus.autoscaling.common.DeleteScheduledActionType;
import com.eucalyptus.autoscaling.common.DeleteTagsResponseType;
import com.eucalyptus.autoscaling.common.DeleteTagsType;
import com.eucalyptus.autoscaling.common.DescribeAdjustmentTypesResponseType;
import com.eucalyptus.autoscaling.common.DescribeAdjustmentTypesType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingInstancesResponseType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingInstancesType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingNotificationTypesResponseType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingNotificationTypesType;
import com.eucalyptus.autoscaling.common.DescribeLaunchConfigurationsResponseType;
import com.eucalyptus.autoscaling.common.DescribeLaunchConfigurationsType;
import com.eucalyptus.autoscaling.common.DescribeMetricCollectionTypesResponseType;
import com.eucalyptus.autoscaling.common.DescribeMetricCollectionTypesType;
import com.eucalyptus.autoscaling.common.DescribeNotificationConfigurationsResponseType;
import com.eucalyptus.autoscaling.common.DescribeNotificationConfigurationsType;
import com.eucalyptus.autoscaling.common.DescribePoliciesResponseType;
import com.eucalyptus.autoscaling.common.DescribePoliciesType;
import com.eucalyptus.autoscaling.common.DescribeScalingActivitiesResponseType;
import com.eucalyptus.autoscaling.common.DescribeScalingActivitiesType;
import com.eucalyptus.autoscaling.common.DescribeScalingProcessTypesResponseType;
import com.eucalyptus.autoscaling.common.DescribeScalingProcessTypesType;
import com.eucalyptus.autoscaling.common.DescribeScheduledActionsResponseType;
import com.eucalyptus.autoscaling.common.DescribeScheduledActionsType;
import com.eucalyptus.autoscaling.common.DescribeTagsResponseType;
import com.eucalyptus.autoscaling.common.DescribeTagsType;
import com.eucalyptus.autoscaling.common.DescribeTerminationPolicyTypesResponseType;
import com.eucalyptus.autoscaling.common.DescribeTerminationPolicyTypesType;
import com.eucalyptus.autoscaling.common.DisableMetricsCollectionResponseType;
import com.eucalyptus.autoscaling.common.DisableMetricsCollectionType;
import com.eucalyptus.autoscaling.common.EnableMetricsCollectionResponseType;
import com.eucalyptus.autoscaling.common.EnableMetricsCollectionType;
import com.eucalyptus.autoscaling.common.ExecutePolicyResponseType;
import com.eucalyptus.autoscaling.common.ExecutePolicyType;
import com.eucalyptus.autoscaling.common.Instance;
import com.eucalyptus.autoscaling.common.Instances;
import com.eucalyptus.autoscaling.common.LaunchConfigurationType;
import com.eucalyptus.autoscaling.common.PutNotificationConfigurationResponseType;
import com.eucalyptus.autoscaling.common.PutNotificationConfigurationType;
import com.eucalyptus.autoscaling.common.PutScalingPolicyResponseType;
import com.eucalyptus.autoscaling.common.PutScalingPolicyType;
import com.eucalyptus.autoscaling.common.PutScheduledUpdateGroupActionResponseType;
import com.eucalyptus.autoscaling.common.PutScheduledUpdateGroupActionType;
import com.eucalyptus.autoscaling.common.ResumeProcessesResponseType;
import com.eucalyptus.autoscaling.common.ResumeProcessesType;
import com.eucalyptus.autoscaling.common.ScalingPolicyType;
import com.eucalyptus.autoscaling.common.SetDesiredCapacityResponseType;
import com.eucalyptus.autoscaling.common.SetDesiredCapacityType;
import com.eucalyptus.autoscaling.common.SetInstanceHealthResponseType;
import com.eucalyptus.autoscaling.common.SetInstanceHealthType;
import com.eucalyptus.autoscaling.common.SuspendProcessesResponseType;
import com.eucalyptus.autoscaling.common.SuspendProcessesType;
import com.eucalyptus.autoscaling.common.TagDescription;
import com.eucalyptus.autoscaling.common.TagDescriptionList;
import com.eucalyptus.autoscaling.common.TagType;
import com.eucalyptus.autoscaling.common.TerminateInstanceInAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.TerminateInstanceInAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.UpdateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.UpdateAutoScalingGroupType;
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration;
import com.eucalyptus.autoscaling.configurations.LaunchConfigurations;
import com.eucalyptus.autoscaling.configurations.PersistenceLaunchConfigurations;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.autoscaling.groups.AutoScalingGroups;
import com.eucalyptus.autoscaling.groups.HealthCheckType;
import com.eucalyptus.autoscaling.groups.PersistenceAutoScalingGroups;
import com.eucalyptus.autoscaling.groups.TerminationPolicyType;
import com.eucalyptus.autoscaling.instances.AutoScalingInstance;
import com.eucalyptus.autoscaling.instances.AutoScalingInstances;
import com.eucalyptus.autoscaling.instances.HealthStatus;
import com.eucalyptus.autoscaling.instances.PersistenceAutoScalingInstances;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataNotFoundException;
import com.eucalyptus.autoscaling.policies.AdjustmentType;
import com.eucalyptus.autoscaling.policies.PersistenceScalingPolicies;
import com.eucalyptus.autoscaling.policies.ScalingPolicies;
import com.eucalyptus.autoscaling.policies.ScalingPolicy;
import com.eucalyptus.autoscaling.tags.AutoScalingGroupTag;
import com.eucalyptus.autoscaling.tags.Tag;
import com.eucalyptus.autoscaling.tags.TagSupport;
import com.eucalyptus.autoscaling.tags.Tags;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Numbers;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Enums;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

public class AutoScalingService {
  private static final Logger logger = Logger.getLogger( AutoScalingService.class );

  private static final Set<String> reservedPrefixes =
      ImmutableSet.<String>builder().add("aws:").add("euca:").build();

  public static long MAX_TAGS_PER_RESOURCE = 10;

  private final LaunchConfigurations launchConfigurations;
  private final AutoScalingGroups autoScalingGroups;
  private final AutoScalingInstances autoScalingInstances;
  private final ScalingPolicies scalingPolicies;
  private final ActivityManager activityManager;
  
  public AutoScalingService() {
    this( 
        new PersistenceLaunchConfigurations( ),
        new PersistenceAutoScalingGroups( ),
        new PersistenceAutoScalingInstances( ),
        new PersistenceScalingPolicies( ),
        new ActivityManager() );
  }

  protected AutoScalingService( final LaunchConfigurations launchConfigurations,
                                final AutoScalingGroups autoScalingGroups,
                                final AutoScalingInstances autoScalingInstances,
                                final ScalingPolicies scalingPolicies,
                                final ActivityManager activityManager ) {
    this.launchConfigurations = launchConfigurations;
    this.autoScalingGroups = autoScalingGroups;
    this.autoScalingInstances = autoScalingInstances;
    this.scalingPolicies = scalingPolicies;
    this.activityManager = activityManager;
  }

  public DescribeAutoScalingGroupsResponseType describeAutoScalingGroups( final DescribeAutoScalingGroupsType request ) throws EucalyptusCloudException {
    final DescribeAutoScalingGroupsResponseType reply = request.getReply( );

    //TODO:STEVE: MaxRecords / NextToken support for DescribeAutoScalingGroups

    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.autoScalingGroupNames().remove( "verbose" );
    final OwnerFullName ownerFullName = ctx.hasAdministrativePrivileges( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    final Predicate<AutoScalingGroupMetadata> requestedAndAccessible = 
        AutoScalingMetadatas.filterPrivilegesByIdOrArn( request.autoScalingGroupNames() );

    try {
      final List<AutoScalingGroupType> results = reply.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember();
      final List<AutoScalingGroup> groups = autoScalingGroups.list( ownerFullName, requestedAndAccessible );
      final Map<String,List<Tag>> tagsMap = TagSupport.forResourceClass( AutoScalingGroup.class )
          .getResourceTagMap( ctx.getUserFullName().asAccountFullName(),
              Iterables.transform( groups, AutoScalingMetadatas.toDisplayName() ), Predicates.alwaysTrue() );

      for ( final AutoScalingGroup autoScalingGroup : groups ) {
        final AutoScalingGroupType type = TypeMappers.transform( autoScalingGroup, AutoScalingGroupType.class );
        final Instances instances = new Instances();
        Iterables.addAll( instances.getMember(),
            Iterables.transform(
                autoScalingInstances.listByGroup( autoScalingGroup ),
                TypeMappers.lookup( AutoScalingInstance.class, Instance.class ) ) );        
        if ( !instances.getMember().isEmpty() ) {
          type.setInstances( instances );
        }
        final TagDescriptionList tags = new TagDescriptionList();
        Tags.addFromTags( tags.getMember(), TagDescription.class, tagsMap.get( autoScalingGroup.getAutoScalingGroupName() ) );
        if ( !tags.getMember().isEmpty() ) {
          type.setTags( tags );
        }
        results.add( type );
      }
    } catch ( Exception e ) {
      handleException( e );
    }    
    
    return reply;
  }

  public EnableMetricsCollectionResponseType enableMetricsCollection(EnableMetricsCollectionType request) throws EucalyptusCloudException {
    EnableMetricsCollectionResponseType reply = request.getReply( );
    return reply;
  }

  public ResumeProcessesResponseType resumeProcesses(ResumeProcessesType request) throws EucalyptusCloudException {
    ResumeProcessesResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteLaunchConfigurationResponseType deleteLaunchConfiguration( final DeleteLaunchConfigurationType request ) throws EucalyptusCloudException {
    final DeleteLaunchConfigurationResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    try {
      final LaunchConfiguration launchConfiguration = launchConfigurations.lookup( 
          ctx.getUserFullName( ).asAccountFullName( ), 
          request.getLaunchConfigurationName( ) );
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

    //TODO:STEVE: MaxRecords / NextToken support for DescribePolicies

    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.policyNames().remove( "verbose" );
    final OwnerFullName ownerFullName = ctx.hasAdministrativePrivileges( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final Predicate<ScalingPolicy> requestedAndAccessible =
        Predicates.and( 
          AutoScalingMetadatas.filterPrivilegesByIdOrArn( request.policyNames() ),
          AutoScalingResourceName.isResourceName().apply( request.getAutoScalingGroupName() ) ?
            AutoScalingMetadatas.filterByProperty(
                  AutoScalingResourceName.parse( request.getAutoScalingGroupName(), autoScalingGroup ).getUuid(),
                  ScalingPolicies.toGroupUuid() )  :
            AutoScalingMetadatas.filterByProperty( 
                request.getAutoScalingGroupName(), 
                ScalingPolicies.toGroupName() )
        );

      final List<ScalingPolicyType> results = reply.getDescribePoliciesResult().getScalingPolicies().getMember();
      for ( final ScalingPolicy scalingPolicy : scalingPolicies.list( ownerFullName, requestedAndAccessible ) ) {
        results.add( TypeMappers.transform( scalingPolicy, ScalingPolicyType.class ) );
      }
    } catch ( Exception e ) {
      handleException( e );
    }

    return reply;
  }

  public DescribeScalingProcessTypesResponseType describeScalingProcessTypes(DescribeScalingProcessTypesType request) throws EucalyptusCloudException {
    DescribeScalingProcessTypesResponseType reply = request.getReply( );
    return reply;
  }

  public CreateAutoScalingGroupResponseType createAutoScalingGroup( final CreateAutoScalingGroupType request ) throws EucalyptusCloudException {
    final CreateAutoScalingGroupResponseType reply = request.getReply( );

    final Context ctx = Contexts.lookup( );

    if ( request.getTags() != null ) {
      for ( final TagType tagType : request.getTags().getMember() ) {
        final String key = tagType.getKey();
        if ( com.google.common.base.Strings.isNullOrEmpty( key ) || key.trim().length() > 128 || isReserved( key ) ) {
          throw new InvalidParameterValueException( "Invalid key (max length 128, must not be empty, reserved prefixes "+reservedPrefixes+"): "+key );
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
          final AutoScalingGroups.PersistingBuilder builder = autoScalingGroups.create(
              ctx.getUserFullName(),
              request.getAutoScalingGroupName(),
              launchConfigurations.lookup( ctx.getUserFullName().asAccountFullName(), request.getLaunchConfigurationName() ),
              Numbers.intValue( request.getMinSize() ),
              Numbers.intValue( request.getMaxSize() ) )
              .withAvailabilityZones( request.availabilityZones() )
              .withDefaultCooldown( Numbers.intValue( request.getDefaultCooldown() ) )
              .withDesiredCapacity( Numbers.intValue( request.getDesiredCapacity() ) )
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

          final List<String> referenceErrors = activityManager.validateReferences(
              ctx.getUserFullName(),
              request.availabilityZones(),
              request.loadBalancerNames()
          );
          if ( !referenceErrors.isEmpty() ) {
            throw Exceptions.toUndeclared( new InvalidParameterValueException( "Invalid parameters " + referenceErrors ) );
          }

          //TODO:STEVE: input validation
          return builder.persist();
        } catch ( AutoScalingMetadataNotFoundException e ) {
          throw Exceptions.toUndeclared( new InvalidParameterValueException( "Launch configuration not found: " + request.getLaunchConfigurationName() ) );
        } catch ( IllegalArgumentException e ) {
          throw Exceptions.toUndeclared( new InvalidParameterValueException( "Invalid health check type: " + request.getHealthCheckType() ) );
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

  public DescribeScalingActivitiesResponseType describeScalingActivities(DescribeScalingActivitiesType request) throws EucalyptusCloudException {
    DescribeScalingActivitiesResponseType reply = request.getReply( );
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

    //TODO:STEVE: MaxRecords / NextToken support for DescribeTags
    //TODO:STEVE: Filtering support for DescribeTags

    final Context context = Contexts.lookup();

    final Ordering<Tag> ordering = Ordering.natural().onResultOf( Tags.resourceId() )
        .compound( Ordering.natural().onResultOf( Tags.key() ) )
        .compound( Ordering.natural().onResultOf( Tags.value() ) );
    try {
      final TagDescriptionList tagDescriptions = new TagDescriptionList();
      for ( final Tag tag : ordering.sortedCopy( Tags.list(
          context.getUserFullName().asAccountFullName(),
          Predicates.alwaysTrue(),
          Restrictions.conjunction(),
          Collections.<String,String>emptyMap() ) ) ) {
        if ( Permissions.isAuthorized(
            PolicySpec.VENDOR_AUTOSCALING,
            tag.getResourceType(),
            tag.getKey(),
            context.getAccount(),
            PolicySpec.describeAction( PolicySpec.VENDOR_AUTOSCALING, tag.getResourceType() ),
            context.getUser() ) ) {
          tagDescriptions.getMember().add( TypeMappers.transform( tag, TagDescription.class ) );
        }
      }
      if ( !tagDescriptions.getMember().isEmpty() ) {
        reply.getDescribeTagsResult().setTags( tagDescriptions );
      }
    } catch ( NoSuchMetadataException e ) {
      handleException( e );
    }

    return reply;
  }

  public ExecutePolicyResponseType executePolicy(final ExecutePolicyType request) throws EucalyptusCloudException {
    final ExecutePolicyResponseType reply = request.getReply( );
    
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    try {
      final ScalingPolicy scalingPolicy;
      try {
        scalingPolicy = scalingPolicies.lookup( 
          accountFullName,
          request.getAutoScalingGroupName(),
          request.getPolicyName() );        
      } catch ( AutoScalingMetadataNotFoundException e ) {
        throw new InvalidParameterValueException( "Scaling policy not found: " + request.getPolicyName() );
      }

      autoScalingGroups.update( accountFullName, request.getAutoScalingGroupName(), new Callback<AutoScalingGroup>(){
        @Override
        public void fire( final AutoScalingGroup autoScalingGroup ) {
          failIfScaling( activityManager, autoScalingGroup );
          setDesiredCapacityWithCooldown(
              autoScalingGroup,
              request.getHonorCooldown(),
              scalingPolicy.getCooldown(),
              scalingPolicy.getAdjustmentType().adjustCapacity(
                  autoScalingGroup.getDesiredCapacity(),
                  scalingPolicy.getScalingAdjustment(),
                  Objects.firstNonNull( scalingPolicy.getMinAdjustmentStep(), 0 ),
                  Objects.firstNonNull( autoScalingGroup.getMinSize(), 0 ),
                  Objects.firstNonNull( autoScalingGroup.getMaxSize(), Integer.MAX_VALUE )
              ) );
        }
      } );
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
        throw new InvalidParameterValueException( "Invalid key (max length 128, must not be empty, reserved prefixes "+reservedPrefixes+"): "+key );
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
                  context.getUser() ) ) {
                Tags.delete( example );
              }
            } catch ( NoSuchMetadataException e ) {
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
            //TODO:STEVE: input validation
            // You will get a ValidationError if you use MinAdjustmentStep on a policy with an AdjustmentType other than PercentChangeInCapacity. 
            if ( request.getAdjustmentType() != null )
              scalingPolicy.setAdjustmentType( 
                  Enums.valueOfFunction( AdjustmentType.class ).apply( request.getAdjustmentType() ) );
            if ( request.getScalingAdjustment() != null )
              scalingPolicy.setScalingAdjustment( request.getScalingAdjustment() );
            if ( request.getCooldown() != null )
              scalingPolicy.setCooldown( request.getCooldown() );
            if ( request.getMinAdjustmentStep() != null )
              scalingPolicy.setMinAdjustmentStep( request.getMinAdjustmentStep() );
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
            final ScalingPolicies.PersistingBuilder builder = scalingPolicies.create(
                ctx.getUserFullName( ),
                autoScalingGroups.lookup( accountFullName, request.getAutoScalingGroupName() ),
                request.getPolicyName(),
                Enums.valueOfFunction( AdjustmentType.class ).apply( request.getAdjustmentType() ),
                request.getScalingAdjustment() )
                .withCooldown( request.getCooldown() )
                .withMinAdjustmentStep( request.getMinAdjustmentStep() );

            //TODO:STEVE: input validation
            // No Auto Scaling name, including policy names, can contain the colon (:) character because colons serve as delimiters in ARNs.
            // You will get a ValidationError if you use MinAdjustmentStep on a policy with an AdjustmentType other than PercentChangeInCapacity. 
            return builder.persist();
          } catch ( AutoScalingMetadataNotFoundException e ) {
            throw Exceptions.toUndeclared( new InvalidParameterValueException( "Auto scaling group not found: " + request.getAutoScalingGroupName() ) );
          } catch ( IllegalArgumentException e ) {
            throw Exceptions.toUndeclared( new InvalidParameterValueException( "Invalid adjustment type: " + request.getAdjustmentType() ) );
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
      final ScalingPolicy scalingPolicy = scalingPolicies.lookup(
          ctx.getUserFullName( ).asAccountFullName( ),
          request.getAutoScalingGroupName(),
          request.getPolicyName( ) );
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
    final OwnerFullName ownerFullName = ctx.hasAdministrativePrivileges( ) ?
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
      throw new InvalidParameterValueException( "Auto scaling instance not found: " + request.getInstanceId( ) );
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
        throw new InvalidParameterValueException( "Invalid key (max length 128, must not be empty, reserved prefixes "+reservedPrefixes+"): "+key );
      }
      if ( value.length() > 256 || isReserved( key ) ) {
        throw new InvalidParameterValueException( "Invalid value (max length 256, reserved prefixes "+reservedPrefixes+"): "+value );
      }
    }

    if ( request.getTags().getMember().size() > 0 ) {
      final Predicate<Void> creator = new Predicate<Void>(){
        @Override
        public boolean apply( final Void v ) {
          try {
            for ( final TagType tagType : request.getTags().getMember() ) {
              final TagSupport tagSupport = TagSupport.fromResourceType( tagType.getResourceType() );
              AutoScalingMetadata resource = tagSupport.lookup( accountFullName, tagType.getResourceId() );

              if ( !RestrictedTypes.filterPrivileged().apply( resource ) ) {
                throw Exceptions.toUndeclared( new InvalidParameterValueException( "Resource not found " + tagType.getResourceId() ) );
              }

              final Tag example = tagSupport.example( resource, accountFullName, null, null );
              if ( Entities.count( example ) >= MAX_TAGS_PER_RESOURCE ) {
                throw Exceptions.toUndeclared( new LimitExceededException("Tag limit exceeded for resource '"+resource.getDisplayName()+"'") );
              }

              final String key = com.google.common.base.Strings.nullToEmpty( tagType.getKey() ).trim();
              final String value = com.google.common.base.Strings.nullToEmpty( tagType.getValue() ).trim();
              final Boolean propagateAtLaunch = Objects.firstNonNull( tagType.getPropagateAtLaunch(), Boolean.FALSE );
              tagSupport.createOrUpdate( resource, ownerFullName, key, value, propagateAtLaunch );
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

  public SuspendProcessesResponseType suspendProcesses(SuspendProcessesType request) throws EucalyptusCloudException {
    SuspendProcessesResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeAutoScalingInstancesResponseType describeAutoScalingInstances( final DescribeAutoScalingInstancesType request ) throws EucalyptusCloudException {
    final DescribeAutoScalingInstancesResponseType reply = request.getReply( );

    //TODO:STEVE: MaxRecords / NextToken support for DescribeAutoScalingInstances

    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.instanceIds().remove( "verbose" );
    final OwnerFullName ownerFullName = ctx.hasAdministrativePrivileges( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    final Predicate<AutoScalingInstanceMetadata> requestedAndAccessible =
        AutoScalingMetadatas.filterPrivilegesById( request.instanceIds() );

    try {
      final List<AutoScalingInstanceDetails> results = 
          reply.getDescribeAutoScalingInstancesResult().getAutoScalingInstances().getMember();
      for ( final AutoScalingInstance autoScalingInstance : autoScalingInstances.list( ownerFullName, requestedAndAccessible ) ) {
        results.add( TypeMappers.transform( autoScalingInstance, AutoScalingInstanceDetails.class ) );
      }
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
              request.getKeyName(),
              request.getSecurityGroups() == null ? Collections.<String>emptyList() : request.getSecurityGroups().getMember()
          );
          if ( !referenceErrors.isEmpty() ) {
            throw Exceptions.toUndeclared( new InvalidParameterValueException( "Invalid parameters " + referenceErrors ) );
          }

          //TODO:STEVE: input validation
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
      final AutoScalingGroup autoScalingGroup = autoScalingGroups.lookup(
          ctx.getUserFullName( ).asAccountFullName( ),
          request.getAutoScalingGroupName() );
      if ( RestrictedTypes.filterPrivileged().apply( autoScalingGroup ) ) {
        // Terminate instances first if requested (but don't wait for success ...)
        if ( Objects.firstNonNull( request.getForceDelete(), Boolean.FALSE ) ) {
          final List<AutoScalingInstance> instances = autoScalingInstances.listByGroup( autoScalingGroup );
          if ( !instances.isEmpty() ) {
            final List<ScalingActivity> activities =
                activityManager.terminateInstances( autoScalingGroup, instances );
            if ( activities==null || activities.isEmpty() ) {
              throw new ScalingActivityInProgressException("Scaling activity in progress");
            }
            autoScalingInstances.deleteByGroup( autoScalingGroup );
          }
        } else {
          failIfScaling( activityManager, autoScalingGroup );
        }
        autoScalingGroups.delete( autoScalingGroup );
      } // else treat this as though the group does not exist
    } catch ( AutoScalingMetadataNotFoundException e ) {
      // so nothing to delete, move along      
    } catch ( Exception e ) {
      handleException( e );
    }
    
    return reply;
  }

  public DisableMetricsCollectionResponseType disableMetricsCollection(DisableMetricsCollectionType request) throws EucalyptusCloudException {
    DisableMetricsCollectionResponseType reply = request.getReply( );
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
              autoScalingGroup.setDefaultCooldown( Numbers.intValue( request.getDefaultCooldown() ) );
            if ( request.getDesiredCapacity() != null )
              autoScalingGroup.updateDesiredCapacity( Numbers.intValue( request.getDesiredCapacity() ) );
            if ( request.getHealthCheckGracePeriod() != null )
              autoScalingGroup.setHealthCheckGracePeriod( Numbers.intValue( request.getHealthCheckGracePeriod() ) );
            if ( request.getHealthCheckType() != null )
              autoScalingGroup.setHealthCheckType( Enums.valueOfFunction( HealthCheckType.class ).apply( request.getHealthCheckType() ) );
            if ( request.getLaunchConfigurationName() != null )
              try {
                autoScalingGroup.setLaunchConfiguration( launchConfigurations.lookup( accountFullName, request.getLaunchConfigurationName() ) );
              } catch ( AutoScalingMetadataNotFoundException e ) {
                throw Exceptions.toUndeclared( new InvalidParameterValueException( "Launch configuration not found: " + request.getLaunchConfigurationName() ) );
              } catch ( AutoScalingMetadataException e ) {
                throw Exceptions.toUndeclared( e );
              }
            if ( request.getMaxSize() != null )
              autoScalingGroup.setMaxSize( Numbers.intValue( request.getMaxSize() ) );
            if ( request.getMinSize() != null )
              autoScalingGroup.setMinSize( Numbers.intValue( request.getMinSize() ) );
            if ( request.terminationPolicies() != null && !request.terminationPolicies().isEmpty() )
              autoScalingGroup.setTerminationPolicies( Lists.newArrayList(
                  Sets.newLinkedHashSet( Iterables.filter(
                      Iterables.transform( request.terminationPolicies(), Enums.valueOfFunction( TerminationPolicyType.class ) ),
                      Predicates.not( Predicates.isNull() ) ) ) ) );
            //TODO:STEVE: something for VPC zone identifier or placement group?

            final List<String> referenceErrors = activityManager.validateReferences(
                autoScalingGroup.getOwner(),
                autoScalingGroup.getAvailabilityZones(),
                Collections.<String>emptyList() // load balancer names cannot be updated
            );
            if ( !referenceErrors.isEmpty() ) {
              throw Exceptions.toUndeclared( new InvalidParameterValueException( "Invalid parameters " + referenceErrors ) );
            }
          }
        }
      };

      autoScalingGroups.update(
          accountFullName,
          request.getAutoScalingGroupName(),
          groupCallback );
    } catch ( AutoScalingMetadataNotFoundException e ) {
      throw new InvalidParameterValueException( "Auto scaling group not found: " + request.getAutoScalingGroupName() );
    } catch ( Exception e ) {
      handleException( e );
    }
    
    return reply;
  }

  public DescribeLaunchConfigurationsResponseType describeLaunchConfigurations(DescribeLaunchConfigurationsType request) throws EucalyptusCloudException {
    final DescribeLaunchConfigurationsResponseType reply = request.getReply( );

    //TODO:STEVE: MaxRecords / NextToken support for DescribeLaunchConfigurations
    
    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.launchConfigurationNames().remove( "verbose" );  
    final OwnerFullName ownerFullName = ctx.hasAdministrativePrivileges( ) &&  showAll ? 
        null : 
        ctx.getUserFullName( ).asAccountFullName( );

    final Predicate<LaunchConfigurationMetadata> requestedAndAccessible =
        AutoScalingMetadatas.filterPrivilegesByIdOrArn( request.launchConfigurationNames() );

    try {
      final List<LaunchConfigurationType> results = reply.getDescribeLaunchConfigurationsResult( ).getLaunchConfigurations().getMember();
      for ( final LaunchConfiguration launchConfiguration : launchConfigurations.list( ownerFullName, requestedAndAccessible ) ) {
        results.add( TypeMappers.transform( launchConfiguration, LaunchConfigurationType.class ) );
      }
    } catch ( Exception e ) {
      handleException( e );
    }

    return reply;
  }

  public DescribeAdjustmentTypesResponseType describeAdjustmentTypes(final DescribeAdjustmentTypesType request) throws EucalyptusCloudException {
    final DescribeAdjustmentTypesResponseType reply = request.getReply( );

    reply.getDescribeAdjustmentTypesResult().setAdjustmentTypes(
        Collections2.transform(
            Collections2.filter( EnumSet.allOf( AdjustmentType.class ), RestrictedTypes.filterPrivilegedWithoutOwner() ),
            Strings.toStringFunction() ) );

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

  public DescribeMetricCollectionTypesResponseType describeMetricCollectionTypes(DescribeMetricCollectionTypesType request) throws EucalyptusCloudException {
    DescribeMetricCollectionTypesResponseType reply = request.getReply( );
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
            setDesiredCapacityWithCooldown( 
                autoScalingGroup, 
                request.getHonorCooldown(), 
                null, 
                Numbers.intValue( request.getDesiredCapacity() ) );
          } 
        }
      };

      autoScalingGroups.update(
          ctx.getUserFullName().asAccountFullName(),
          request.getAutoScalingGroupName(),
          groupCallback);
    } catch ( AutoScalingMetadataNotFoundException e ) {
      throw new InvalidParameterValueException( "Auto scaling group not found: " + request.getAutoScalingGroupName() );
    } catch ( Exception e ) {
      handleException( e );
    }    
    
    return reply;
  }

  public TerminateInstanceInAutoScalingGroupResponseType terminateInstanceInAutoScalingGroup( final TerminateInstanceInAutoScalingGroupType request ) throws EucalyptusCloudException {
    final TerminateInstanceInAutoScalingGroupResponseType reply = request.getReply( );
    
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.hasAdministrativePrivileges( ) ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final AutoScalingInstance instance = 
          autoScalingInstances.lookup( ownerFullName, request.getInstanceId() );
      if ( !RestrictedTypes.filterPrivileged().apply( instance ) ) {
        throw new AutoScalingMetadataNotFoundException("Instance not found");
      }
      
      final List<ScalingActivity> activities = 
          activityManager.terminateInstances( instance.getAutoScalingGroup(), Lists.newArrayList( instance ) );
      if ( activities == null || activities.isEmpty() ) {
        throw new ScalingActivityInProgressException("Scaling activity in progress");
      }
      reply.getTerminateInstanceInAutoScalingGroupResult().setActivity( 
        TypeMappers.transform( activities.get( 0 ), Activity.class )  
      );

      if ( Objects.firstNonNull( request.getShouldDecrementDesiredCapacity(), Boolean.FALSE ) ) {
        final String groupArn = instance.getAutoScalingGroup().getArn();
        final Callback<AutoScalingGroup> groupCallback = new Callback<AutoScalingGroup>() {
          @Override
          public void fire( final AutoScalingGroup autoScalingGroup ) {
            autoScalingGroup.setDesiredCapacity( autoScalingGroup.getDesiredCapacity() - 1 );
          }
        };

        autoScalingGroups.update(
            ctx.getUserFullName().asAccountFullName(),
            groupArn,
            groupCallback);
      }
    } catch ( AutoScalingMetadataNotFoundException e ) {
      throw new InvalidParameterValueException( "Auto scaling instance not found: " + request.getInstanceId( ) );
    } catch ( Exception e ) {
      handleException( e );
    }
    
    return reply;
  }

  private static void failIfScaling( final ActivityManager activityManager,
                                     final AutoScalingGroup group ) {
    if ( activityManager.scalingInProgress( group ) ) {
      throw Exceptions.toUndeclared( new ScalingActivityInProgressException("Scaling activity in progress") );      
    }
  }

  private static void setDesiredCapacityWithCooldown( final AutoScalingGroup autoScalingGroup,
                                                      final Boolean honorCooldown,
                                                      final Integer cooldown,
                                                      final int capacity ) {
    final long cooldownMs = TimeUnit.SECONDS.toMillis( Objects.firstNonNull( cooldown, autoScalingGroup.getDefaultCooldown() ) );
    if ( !Objects.firstNonNull( honorCooldown, Boolean.FALSE ) ||
        ( System.currentTimeMillis() - autoScalingGroup.getCapacityTimestamp().getTime() ) > cooldownMs ) {
      autoScalingGroup.updateDesiredCapacity( capacity );
      autoScalingGroup.setCapacityTimestamp( new Date() );
    } else {
      throw Exceptions.toUndeclared( new InternalFailureException("Group is in cooldown") );
    }
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
      throw new InvalidParameterValueException( invalidResourceNameException.getMessage() );
    }

    logger.error( e, e );

    final InternalFailureException exception = new InternalFailureException( String.valueOf(e.getMessage()) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );      
    }
    throw exception;
  }
}
