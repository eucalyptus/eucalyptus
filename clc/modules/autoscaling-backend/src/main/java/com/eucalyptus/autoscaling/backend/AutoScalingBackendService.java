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
package com.eucalyptus.autoscaling.backend;

import static com.eucalyptus.autoscaling.common.AutoScalingResourceName.InvalidResourceNameException;
import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingGroupMetadata;
import static com.eucalyptus.autoscaling.common.AutoScalingResourceName.Type.autoScalingGroup;
import static com.google.common.base.Strings.nullToEmpty;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;

import com.eucalyptus.auth.AuthQuotaException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.autoscaling.activities.ActivityManager;
import com.eucalyptus.autoscaling.common.internal.activities.ScalingActivity;
import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
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
import com.eucalyptus.autoscaling.common.backend.msgs.DeletePolicyResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeletePolicyType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeleteTagsResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DeleteTagsType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribePoliciesResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DescribePoliciesType;
import com.eucalyptus.autoscaling.common.backend.msgs.DisableMetricsCollectionResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.DisableMetricsCollectionType;
import com.eucalyptus.autoscaling.common.backend.msgs.EnableMetricsCollectionResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.EnableMetricsCollectionType;
import com.eucalyptus.autoscaling.common.backend.msgs.ExecutePolicyResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.ExecutePolicyType;
import com.eucalyptus.autoscaling.common.backend.msgs.PutScalingPolicyResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.PutScalingPolicyType;
import com.eucalyptus.autoscaling.common.backend.msgs.ResumeProcessesResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.ResumeProcessesType;
import com.eucalyptus.autoscaling.common.backend.msgs.SetDesiredCapacityResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.SetDesiredCapacityType;
import com.eucalyptus.autoscaling.common.backend.msgs.SetInstanceHealthResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.SetInstanceHealthType;
import com.eucalyptus.autoscaling.common.backend.msgs.SetInstanceProtectionResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.SetInstanceProtectionType;
import com.eucalyptus.autoscaling.common.backend.msgs.SuspendProcessesResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.SuspendProcessesType;
import com.eucalyptus.autoscaling.common.backend.msgs.TerminateInstanceInAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.TerminateInstanceInAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.backend.msgs.UpdateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.backend.msgs.UpdateAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.Activity;
import com.eucalyptus.autoscaling.common.msgs.Alarms;
import com.eucalyptus.autoscaling.common.msgs.BlockDeviceMappingType;
import com.eucalyptus.autoscaling.common.msgs.ScalingPolicyType;
import com.eucalyptus.autoscaling.common.msgs.TagType;
import com.eucalyptus.autoscaling.common.policy.AutoScalingPolicySpec;
import com.eucalyptus.autoscaling.config.AutoScalingConfiguration;
import com.eucalyptus.autoscaling.common.internal.configurations.LaunchConfiguration;
import com.eucalyptus.autoscaling.common.internal.configurations.LaunchConfigurationMinimumView;
import com.eucalyptus.autoscaling.common.internal.configurations.LaunchConfigurations;
import com.eucalyptus.autoscaling.common.internal.groups.AutoScalingGroup;
import com.eucalyptus.autoscaling.common.internal.groups.AutoScalingGroupCoreView;
import com.eucalyptus.autoscaling.common.internal.groups.AutoScalingGroups;
import com.eucalyptus.autoscaling.common.internal.groups.MetricCollectionType;
import com.eucalyptus.autoscaling.common.internal.groups.HealthCheckType;
import com.eucalyptus.autoscaling.common.internal.groups.ScalingProcessType;
import com.eucalyptus.autoscaling.common.internal.groups.SuspendedProcess;
import com.eucalyptus.autoscaling.common.internal.groups.TerminationPolicyType;
import com.eucalyptus.autoscaling.common.internal.instances.AutoScalingInstance;
import com.eucalyptus.autoscaling.common.internal.instances.AutoScalingInstanceGroupView;
import com.eucalyptus.autoscaling.common.internal.instances.AutoScalingInstances;
import com.eucalyptus.autoscaling.common.internal.instances.HealthStatus;
import com.eucalyptus.autoscaling.common.internal.metadata.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.common.internal.metadata.AutoScalingMetadataNotFoundException;
import com.eucalyptus.autoscaling.common.internal.policies.AdjustmentType;
import com.eucalyptus.autoscaling.common.internal.policies.ScalingPolicies;
import com.eucalyptus.autoscaling.common.internal.policies.ScalingPolicy;
import com.eucalyptus.autoscaling.common.internal.policies.ScalingPolicyView;
import com.eucalyptus.autoscaling.common.internal.tags.AutoScalingGroupTag;
import com.eucalyptus.autoscaling.common.internal.tags.Tag;
import com.eucalyptus.autoscaling.common.internal.tags.TagSupport;
import com.eucalyptus.autoscaling.common.internal.tags.Tags;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Consumers;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.Numbers;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@SuppressWarnings( { "UnusedDeclaration", "Guava", "StaticPseudoFunctionalStyleMethod", "Convert2Lambda", "Convert2streamapi" } )
@ComponentNamed
public class AutoScalingBackendService {
  private static final Logger logger = Logger.getLogger( AutoScalingBackendService.class );

  private static final Set<String> reservedPrefixes =
      ImmutableSet.<String>builder().add("aws:").add("euca:").build();

  private final LaunchConfigurations launchConfigurations;
  private final AutoScalingGroups autoScalingGroups;
  private final AutoScalingInstances autoScalingInstances;
  private final ScalingPolicies scalingPolicies;
  private final ActivityManager activityManager;

  @Inject
  public AutoScalingBackendService( final LaunchConfigurations launchConfigurations,
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
                  FUtils.valueOfFunction( MetricCollectionType.class ) ) );
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
                  FUtils.valueOfFunction(ScalingProcessType.class) ) );
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
    final boolean isGroupResourceName = AutoScalingResourceName.isResourceName().apply( request.getAutoScalingGroupName() );
    final boolean showAll = request.policyNames().remove( "verbose" ) || isGroupResourceName;
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final Predicate<ScalingPolicy> requestedAndAccessible =
        Predicates.and( 
          AutoScalingMetadatas.filterPrivilegesByIdOrArn( ScalingPolicy.class, request.policyNames() ),
          isGroupResourceName ?
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

      if ( request.getTags().getMember().size() > AutoScalingConfiguration.getMaxTags( ) ) {
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

          final Iterable<String> subnetIds = Splitter.on( ',' ).trimResults().omitEmptyStrings().split( nullToEmpty( request.getVpcZoneIdentifier() ) );
          final AtomicReference<Map<String,String>> subnetsByZone = new AtomicReference<>( );
          final List<String> referenceErrors = activityManager.validateReferences(
              ctx.getUserFullName(),
              Consumers.atomic( subnetsByZone ),
              request.availabilityZones(),
              request.loadBalancerNames(),
              request.targetGroupArns(),
              subnetIds
          );
          verifyUnsupportedReferences( referenceErrors, request.getPlacementGroup( ) );

          if ( !referenceErrors.isEmpty() ) {
            throw Exceptions.toUndeclared( new ValidationErrorException( "Invalid parameters " + referenceErrors ) );
          }

          final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
          final AutoScalingGroups.PersistingBuilder builder = autoScalingGroups.create(
              ctx.getUserFullName(),
              request.getAutoScalingGroupName(),
              verifyOwnership( accountFullName, launchConfigurations.lookup( accountFullName, request.getLaunchConfigurationName(), Functions.identity() ) ),
              minSize,
              maxSize )
              .withAvailabilityZones( request.availabilityZones( ) != null && !request.availabilityZones( ).isEmpty( ) ?
                  request.availabilityZones( ) :
                  subnetsByZone.get( ) == null ? null : subnetsByZone.get( ).keySet( )
              )
              .withSubnetsByZone( subnetsByZone.get( ) )
              .withDefaultCooldown( Numbers.intValue( request.getDefaultCooldown() ) )
              .withDesiredCapacity( desiredCapacity )
              .withHealthCheckGracePeriod( Numbers.intValue( request.getHealthCheckGracePeriod() ) )
              .withHealthCheckType(
                  request.getHealthCheckType()==null ? null : HealthCheckType.valueOf( request.getHealthCheckType() ) )
              .withNewInstancesProtectedFromScaleIn( request.getNewInstancesProtectedFromScaleIn( ) )
              .withLoadBalancerNames( request.loadBalancerNames() )
              .withTargetGroupArns( request.targetGroupArns() )
              .withTerminationPolicyTypes( request.terminationPolicies() == null ? null :
                  Collections2.filter( Collections2.transform(
                          request.terminationPolicies(), FUtils.valueOfFunction( TerminationPolicyType.class ) ),
                      Predicates.not( Predicates.isNull() ) ) )
              .withTags( request.getTags() == null ?
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
              MoreObjects.firstNonNull( scalingPolicy.getMinAdjustmentStep( ), 0 ),
              MoreObjects.firstNonNull( autoScalingGroup.getMinSize( ), 0 ),
              MoreObjects.firstNonNull( autoScalingGroup.getMaxSize( ), Integer.MAX_VALUE )
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
    final List<TagType> tagTypes = MoreObjects.firstNonNull( request.getTags().getMember(), Collections.emptyList() );

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
                  AutoScalingPolicySpec.VENDOR_AUTOSCALING,
                  AutoScalingPolicySpec.AUTOSCALING_RESOURCE_TAG,
                  example.getResourceType() + ":" + example.getResourceId() + ":" + example.getKey(),
                  context.getAccount(),
                  AutoScalingPolicySpec.AUTOSCALING_DELETETAGS,
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
                  FUtils.valueOfFunction( AdjustmentType.class ).apply( request.getAdjustmentType() ) );
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
                FUtils.valueOfFunction( AdjustmentType.class ).apply( request.getAdjustmentType() );

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
                verifyOwnership( accountFullName, autoScalingGroups.lookup( accountFullName, request.getAutoScalingGroupName(), Functions.identity() ) ),
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
            if ( !MoreObjects.firstNonNull( request.getShouldRespectGracePeriod(), Boolean.FALSE ) ||
                instance.healthStatusGracePeriodExpired() ) {
              instance.setHealthStatus( FUtils.valueOfFunction( HealthStatus.class ).apply( request.getHealthStatus( ) ) );
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

  public CreateOrUpdateTagsResponseType createOrUpdateTags( final CreateOrUpdateTagsType request ) throws EucalyptusCloudException {
    final CreateOrUpdateTagsResponseType reply = request.getReply( );

    final Context context = Contexts.lookup();
    final UserFullName ownerFullName = context.getUserFullName();
    final AccountFullName accountFullName = ownerFullName.asAccountFullName();

    for ( final TagType tagType : request.getTags().getMember() ) {
      final String key = tagType.getKey();
      final String value = nullToEmpty( tagType.getValue() ).trim();

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

              final String key = nullToEmpty( tagType.getKey() ).trim();
              final String value = nullToEmpty( tagType.getValue() ).trim();
              final Boolean propagateAtLaunch = MoreObjects.firstNonNull( tagType.getPropagateAtLaunch(), Boolean.FALSE );
              tagSupport.createOrUpdate( resource, ownerFullName, key, value, propagateAtLaunch );

              final Tag example = tagSupport.example( resource, accountFullName, null, null );
              if ( Entities.count( example ) > AutoScalingConfiguration.getMaxTags( ) ) {
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
                  FUtils.valueOfFunction(ScalingProcessType.class) ) );
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
            .withSecurityGroups( request.getSecurityGroups() != null ? request.getSecurityGroups().getMember() : null )
            .withAssociatePublicIpAddress( request.getAssociatePublicIpAddress( ) );
            
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
              request.getSecurityGroups() == null ? Collections.emptyList() : request.getSecurityGroups().getMember(),
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
        if ( MoreObjects.firstNonNull( request.getForceDelete(), Boolean.FALSE ) ) {
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
                  FUtils.valueOfFunction(MetricCollectionType.class) ) );
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
            if ( request.getDefaultCooldown() != null )
              autoScalingGroup.setDefaultCooldown( Numbers.intValue( request.getDefaultCooldown( ) ) );
            if ( request.getHealthCheckGracePeriod( ) != null )
              autoScalingGroup.setHealthCheckGracePeriod( Numbers.intValue( request.getHealthCheckGracePeriod( ) ) );
            if ( request.getHealthCheckType( ) != null )
              autoScalingGroup.setHealthCheckType( FUtils.valueOfFunction( HealthCheckType.class ).apply( request.getHealthCheckType( ) ) );
            if ( request.getNewInstancesProtectedFromScaleIn( ) != null )
              autoScalingGroup.setNewInstancesProtectedFromScaleIn( request.getNewInstancesProtectedFromScaleIn( ) );
            if ( request.getLaunchConfigurationName( ) != null )
              try {
                autoScalingGroup.setLaunchConfiguration( verifyOwnership(
                    accountFullName,
                    launchConfigurations.lookup( accountFullName, request.getLaunchConfigurationName(), Functions.identity() ) ) );
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
                      Iterables.transform( request.terminationPolicies( ), FUtils.valueOfFunction( TerminationPolicyType.class ) ),
                      Predicates.not( Predicates.isNull( ) ) ) ) ) );
            if ( request.getDesiredCapacity() != null ||
                ( request.getDesiredCapacity() == null && autoScalingGroup.getDesiredCapacity() < autoScalingGroup.getMinSize( ) ) ||
                ( request.getDesiredCapacity() == null && autoScalingGroup.getDesiredCapacity() > autoScalingGroup.getMaxSize( ) ) ) {
              Integer updatedDesiredCapacity = request.getDesiredCapacity() != null ?
                  Numbers.intValue( request.getDesiredCapacity( ) ) :
                  Math.min(
                      Math.max( autoScalingGroup.getDesiredCapacity( ), autoScalingGroup.getMinSize( ) ),
                      autoScalingGroup.getMaxSize( ) );
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

            final Iterable<String> subnetIds = Splitter.on( ',' ).trimResults().omitEmptyStrings().split( nullToEmpty( request.getVpcZoneIdentifier() ) );
            final AtomicReference<Map<String, String>> subnetsByZone = new AtomicReference<>();
            final List<String> referenceErrors = activityManager.validateReferences(
                autoScalingGroup.getOwner(),
                Consumers.atomic( subnetsByZone ),
                request.availabilityZones( ),
                Collections.emptyList(), // load balancer names cannot be updated
                Collections.emptyList(), // target group arns cannot be updated
                subnetIds
            );
            verifyUnsupportedReferences( referenceErrors, request.getPlacementGroup() );

            if ( !referenceErrors.isEmpty() ) {
              throw Exceptions.toUndeclared( new ValidationErrorException( "Invalid parameters " + referenceErrors ) );
            }

            if ( request.getVpcZoneIdentifier() != null ) {
              autoScalingGroup.setSubnetIdByZone( subnetsByZone.get( ) );
              autoScalingGroup.updateAvailabilityZones( Lists.newArrayList( Sets.newLinkedHashSet(
                  request.availabilityZones( ) != null && !request.availabilityZones( ).isEmpty( ) ?
                      request.availabilityZones( ) :
                      subnetsByZone.get( ).keySet( ) ) ) );
            } else if ( request.availabilityZones() != null && !request.availabilityZones().isEmpty() ) {
              autoScalingGroup.updateAvailabilityZones( Lists.newArrayList( Sets.newLinkedHashSet( request.availabilityZones() ) ) );
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

  public SetInstanceProtectionResponseType setInstanceProtection( SetInstanceProtectionType request) throws EucalyptusCloudException {
    final SetInstanceProtectionResponseType reply = request.getReply( );

    final Context ctx = Contexts.lookup( );
    try {
      final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
      final Callback<AutoScalingGroup> groupCallback = new Callback<AutoScalingGroup>() {
        @Override
        public void fire( final AutoScalingGroup autoScalingGroup ) {
          if ( RestrictedTypes.filterPrivileged().apply( autoScalingGroup ) ) {
            for ( final AutoScalingInstance instance : autoScalingGroup.getAutoScalingInstances( ) ) {
              if ( request.getInstanceIds( ).getMember( ).contains( instance.getInstanceId( ) ) ) {
                instance.setProtectedFromScaleIn( request.getProtectedFromScaleIn( ) );
              }
            }
          }
        }
      };
      autoScalingGroups.update(
          accountFullName,
          request.getAutoScalingGroupName( ),
          groupCallback );
    } catch ( AutoScalingMetadataNotFoundException e ) {
      throw new ValidationErrorException( "Auto scaling group not found: " + request.getAutoScalingGroupName() );
    } catch ( Exception e ) {
      handleException( e );
    }
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
          if ( MoreObjects.firstNonNull( request.getShouldDecrementDesiredCapacity(), Boolean.FALSE ) ) {
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
    final long cooldownMs = TimeUnit.SECONDS.toMillis( MoreObjects.firstNonNull( cooldown, autoScalingGroup.getDefaultCooldown() ) );
    if ( !MoreObjects.firstNonNull( honorCooldown, Boolean.FALSE ) ||
        ( System.currentTimeMillis() - autoScalingGroup.getCapacityTimestamp().getTime() ) > cooldownMs ) {
      autoScalingGroup.updateDesiredCapacity( capacity, reason );
      autoScalingGroup.setCapacityTimestamp( new Date() );
    } else {
      throw Exceptions.toUndeclared( new InternalFailureException("Group is in cooldown") );
    }
  }

  private static void verifyUnsupportedReferences( final List<String> referenceErrors,
                                                   final String placementGroup ) {
    if ( !com.google.common.base.Strings.isNullOrEmpty( placementGroup ) ) {
      referenceErrors.add( "Invalid placement group: " + placementGroup );
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
    return !Contexts.lookup( ).isPrivileged( ) && Iterables.any( reservedPrefixes, Strings.isPrefixOf( text ) );
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
