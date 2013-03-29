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
package com.eucalyptus.autoscaling.activities;

import static com.eucalyptus.autoscaling.activities.BackoffRunner.TaskWithBackOff;
import static com.eucalyptus.autoscaling.activities.ZoneUnavailabilityMarkers.ZoneCallback;
import static com.eucalyptus.autoscaling.instances.AutoScalingInstances.availabilityZone;
import static com.eucalyptus.autoscaling.instances.AutoScalingInstances.instanceId;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.config.AutoScalingConfiguration;
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.autoscaling.groups.AutoScalingGroups;
import com.eucalyptus.autoscaling.groups.GroupScalingCause;
import com.eucalyptus.autoscaling.groups.HealthCheckType;
import com.eucalyptus.autoscaling.groups.MetricCollectionType;
import com.eucalyptus.autoscaling.groups.PersistenceAutoScalingGroups;
import com.eucalyptus.autoscaling.groups.ScalingProcessType;
import com.eucalyptus.autoscaling.groups.SuspendedProcess;
import com.eucalyptus.autoscaling.groups.TerminationPolicyType;
import com.eucalyptus.autoscaling.instances.AutoScalingInstance;
import com.eucalyptus.autoscaling.instances.AutoScalingInstances;
import com.eucalyptus.autoscaling.instances.ConfigurationState;
import com.eucalyptus.autoscaling.instances.HealthStatus;
import com.eucalyptus.autoscaling.instances.LifecycleState;
import com.eucalyptus.autoscaling.instances.PersistenceAutoScalingInstances;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataNotFoundException;
import com.eucalyptus.autoscaling.tags.Tag;
import com.eucalyptus.autoscaling.tags.TagSupport;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cloudwatch.DescribeAlarmsResponseType;
import com.eucalyptus.cloudwatch.DescribeAlarmsType;
import com.eucalyptus.cloudwatch.Dimension;
import com.eucalyptus.cloudwatch.Dimensions;
import com.eucalyptus.cloudwatch.MetricAlarm;
import com.eucalyptus.cloudwatch.MetricData;
import com.eucalyptus.cloudwatch.MetricDatum;
import com.eucalyptus.cloudwatch.PutMetricDataResponseType;
import com.eucalyptus.cloudwatch.PutMetricDataType;
import com.eucalyptus.cloudwatch.ResourceList;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.event.SystemClock;
import com.eucalyptus.loadbalancing.DeregisterInstancesFromLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.DeregisterInstancesFromLoadBalancerType;
import com.eucalyptus.loadbalancing.DescribeInstanceHealthResponseType;
import com.eucalyptus.loadbalancing.DescribeInstanceHealthType;
import com.eucalyptus.loadbalancing.DescribeLoadBalancersResponseType;
import com.eucalyptus.loadbalancing.DescribeLoadBalancersType;
import com.eucalyptus.loadbalancing.Instance;
import com.eucalyptus.loadbalancing.LoadBalancerDescription;
import com.eucalyptus.loadbalancing.LoadBalancerNames;
import com.eucalyptus.loadbalancing.RegisterInstancesWithLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.RegisterInstancesWithLoadBalancerType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.vmtypes.DescribeVmTypesResponseType;
import com.eucalyptus.vmtypes.DescribeVmTypesType;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.ClusterInfoType;
import edu.ucsb.eucalyptus.msgs.CreateTagsResponseType;
import edu.ucsb.eucalyptus.msgs.CreateTagsType;
import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesType;
import edu.ucsb.eucalyptus.msgs.DescribeImagesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeImagesType;
import edu.ucsb.eucalyptus.msgs.DescribeInstanceStatusResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstanceStatusType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsType;
import edu.ucsb.eucalyptus.msgs.DescribeSecurityGroupsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSecurityGroupsType;
import edu.ucsb.eucalyptus.msgs.DescribeTagsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeTagsType;
import edu.ucsb.eucalyptus.msgs.Filter;
import edu.ucsb.eucalyptus.msgs.ImageDetails;
import edu.ucsb.eucalyptus.msgs.InstanceStatusItemType;
import edu.ucsb.eucalyptus.msgs.ResourceTag;
import edu.ucsb.eucalyptus.msgs.RunInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;
import edu.ucsb.eucalyptus.msgs.SecurityGroupItemType;
import edu.ucsb.eucalyptus.msgs.TagInfo;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType;

/**
 * Launches / pokes / times out activities.
 */
public class ActivityManager {
  private static final Logger logger = Logger.getLogger( ActivityManager.class );

  private static final EnumSet<ActivityStatusCode> completedActivityStates = EnumSet.of(
      ActivityStatusCode.Cancelled,
      ActivityStatusCode.Failed,
      ActivityStatusCode.Successful );

  private static final Set<MetricCollectionType> instanceMetrics = EnumSet.of(
      MetricCollectionType.GroupInServiceInstances,
      MetricCollectionType.GroupPendingInstances,
      MetricCollectionType.GroupTerminatingInstances,
      MetricCollectionType.GroupTotalInstances );

  private static final String INSTANCE_PROFILE_RESOURCE =
      PolicySpec.qualifiedName( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_INSTANCE_PROFILE );

  private final ScalingActivities scalingActivities;
  private final AutoScalingGroups autoScalingGroups;
  private final AutoScalingInstances autoScalingInstances;
  private final ZoneUnavailabilityMarkers zoneAvailabilityMarkers;
  private final ZoneMonitor zoneMonitor;
  private final BackoffRunner runner = BackoffRunner.getInstance( );
  private final ConcurrentMap<String,Integer> launchFailureCounters = Maps.newConcurrentMap();
  private final List<UnstableInstanceState> unstableInstanceStates = ImmutableList.<UnstableInstanceState>builder()
      .add( state( LifecycleState.Terminating, ConfigurationState.Instantiated, terminateInstancesTask() ) )
      .add( state( LifecycleState.Terminating, ConfigurationState.Registered, removeFromLoadBalancerOrTerminate() ) )
      .add( state( LifecycleState.InService, ConfigurationState.Instantiated, addToLoadBalancer() )  )
      .build();
  private final List<ScalingTask> scalingTasks = ImmutableList.<ScalingTask>builder()
      .add( new ScalingTask(   30, ActivityTask.Timeout           ) { @Override void doWork( ) throws Exception { timeoutScalingActivities( ); } } )
      .add( new ScalingTask( 3600, ActivityTask.Expiry            ) { @Override void doWork( ) throws Exception { deleteExpiredActivities( ); } } )
      .add( new ScalingTask(   10, ActivityTask.ZoneHealth        ) { @Override void doWork( ) throws Exception { updateUnavailableZones( ); } } )
      .add( new ScalingTask(   10, ActivityTask.Recovery          ) { @Override void doWork( ) throws Exception { progressUnstableStates( ); } } )
      .add( new ScalingTask(   10, ActivityTask.Scaling           ) { @Override void doWork( ) throws Exception { scalingActivities( ); } } )
      .add( new ScalingTask(   10, ActivityTask.Scaling           ) { @Override void doWork( ) throws Exception { replaceUnhealthy( ); } } )
      .add( new ScalingTask(   10, ActivityTask.InstanceCleanup   ) { @Override void doWork( ) throws Exception { runningInstanceChecks( ); } } )
      .add( new ScalingTask(   10, ActivityTask.MetricsSubmission ) { @Override void doWork( ) throws Exception { submitMetrics( ); } } )
      .build( );

  private static UnstableInstanceState state( final LifecycleState lifecycleState,
                                              final ConfigurationState configurationState,
                                              final Function<Iterable<AutoScalingInstance>,? extends ScalingProcessTask<?>> stateProgressFunction ) {
    return new UnstableInstanceState( lifecycleState, configurationState, stateProgressFunction );
  }

  private static final class UnstableInstanceState {
    private final LifecycleState lifecycleState;
    private final ConfigurationState configurationState;
    private final Function<Iterable<AutoScalingInstance>,? extends ScalingProcessTask<?>> stateProgressFunction;

    private UnstableInstanceState( final LifecycleState lifecycleState,
                                   final ConfigurationState configurationState,
                                   final Function<Iterable<AutoScalingInstance>,? extends ScalingProcessTask<?>> stateProgressFunction ) {
      this.lifecycleState = lifecycleState;
      this.configurationState = configurationState;
      this.stateProgressFunction = stateProgressFunction;
    }

    public LifecycleState getLifecycleState() {
      return lifecycleState;
    }

    public ConfigurationState getConfigurationState() {
      return configurationState;
    }

    public Function<Iterable<AutoScalingInstance>, ? extends ScalingProcessTask<?>> getStateProgressFunction() {
      return stateProgressFunction;
    }
  }

  public enum ActivityTask { Timeout, Expiry, ZoneHealth, Recovery, Scaling, InstanceCleanup, MetricsSubmission }

  public ActivityManager() {
    this(
        new PersistenceScalingActivities( ),
        new PersistenceAutoScalingGroups( ),
        new PersistenceAutoScalingInstances( ),
        new PersistenceZoneUnavailabilityMarkers(),
        new ZoneMonitor() );
  }

  protected ActivityManager( final ScalingActivities scalingActivities,
                             final AutoScalingGroups autoScalingGroups,
                             final AutoScalingInstances autoScalingInstances,
                             final ZoneUnavailabilityMarkers zoneAvailabilityMarkers,
                             final ZoneMonitor zoneMonitor ) {
    this.scalingActivities = scalingActivities;
    this.autoScalingGroups = autoScalingGroups;
    this.autoScalingInstances = autoScalingInstances;
    this.zoneAvailabilityMarkers = zoneAvailabilityMarkers;
    this.zoneMonitor = zoneMonitor;
  }

  public void doScaling() {
    for ( final ScalingTask scalingTask : scalingTasks ) {
      try {
        scalingTask.perhapsWork( );
      } catch ( Exception e ) {
        logger.error( e, e );
      }
    }
  }

  public boolean scalingInProgress( final AutoScalingGroup group ) {
    final String arn = group.getArn();
    return taskInProgress( arn );
  }

  @Nullable
  public List<ScalingActivity> terminateInstances( final AutoScalingGroup group,
                                                   final Collection<AutoScalingInstance> instances ) {
    final String arn = group.getArn();
    final Set<String> arnSet = Sets.newHashSet( Iterables.transform( instances, AutoScalingInstances.groupArn() ) );
    if ( arnSet.size()!=1 || !arnSet.iterator().next().equals( arn ) ) {
      throw new IllegalArgumentException( "Instances for termination must belong to the given group." );
    }
    final List<String> instancesToTerminate = Lists.newArrayList();
    Iterables.addAll(
        instancesToTerminate,
        Iterables.transform( instances, AutoScalingInstances.instanceId()) );

    final UserTerminateInstancesScalingProcessTask task =
        new UserTerminateInstancesScalingProcessTask( group, instancesToTerminate );
    runTask( task );
    List<ScalingActivity> activities = task.getActivities();
    if ( activities != null && !activities.isEmpty() ) {
      // termination accepted so fire off de-registration also
      runTask( new UserRemoveFromLoadBalancerScalingProcessTask( group, instancesToTerminate ) );
    }
    return activities;
  }

  public List<String> validateReferences( final OwnerFullName owner,
                                          final Collection<String> availabilityZones,
                                          final Collection<String> loadBalancerNames ) {
    return validateReferences(
        owner,
        Objects.firstNonNull( availabilityZones, Collections.<String>emptyList() ),
        Objects.firstNonNull( loadBalancerNames, Collections.<String>emptyList() ),
        Collections.<String>emptyList(),
        null,
        null,
        Collections.<String>emptyList(),
        null );

  }

  public List<String> validateReferences( final OwnerFullName owner,
                                          final Iterable<String> imageIds,
                                          final String instanceType,
                                          final String keyName,
                                          final Iterable<String> securityGroups,
                                          final String iamInstanceProfile ) {
    return validateReferences(
        owner,
        Collections.<String>emptyList(),
        Collections.<String>emptyList(),
        Objects.firstNonNull( imageIds, Collections.<String>emptyList() ),
        instanceType,
        keyName,
        Objects.firstNonNull( securityGroups, Collections.<String>emptyList() ),
        iamInstanceProfile );
  }

  public Map<String,Collection<String>> getAlarmsForPolicies( final OwnerFullName owner,
                                                              final List<String> policyArns ) {
    final Map<String,Collection<String>> policyArnToAlarmArnMap = Maps.newHashMap();
    final AlarmLookupProcessTask task = new AlarmLookupProcessTask( owner, policyArns );
    runTask( task );
    try {
      final boolean success = task.getFuture().get();
      if ( success ) {
        policyArnToAlarmArnMap.putAll( task.getPolicyArnToAlarmArns() );
      }
    } catch ( ExecutionException e ) {
      logger.error( e, e );
    } catch ( InterruptedException e ) {
      logger.error( e, e );
    }
    return policyArnToAlarmArnMap;
  }

  protected long timestamp() {
    return System.currentTimeMillis();
  }

  /**
   * Periodically executed scaling work.
   *
   * If scaling activities are not updated for some time we will fail them.
   *
   * Activities should not require this cleanup, this is an error case.
   */
  private void timeoutScalingActivities( ) throws AutoScalingMetadataException {
    for ( final ScalingActivity activity : scalingActivities.listByActivityStatusCode( null, completedActivityStates ) ) {
      if ( !completedActivityStates.contains( activity.getStatusCode( ) ) &&
          isTimedOut( activity.getLastUpdateTimestamp() ) ) {
        scalingActivities.update( activity.getOwner(),
            activity.getActivityId(),
            new Callback<ScalingActivity>(){
              @Override
              public void fire( final ScalingActivity scalingActivity ) {
                scalingActivity.setStatusCode( ActivityStatusCode.Cancelled );
                scalingActivity.setEndTime( new Date() );
              }
            } );
      }
    }
  }

  /**
   * Periodically executed scaling work.
   */
  private void deleteExpiredActivities() throws AutoScalingMetadataException {
    logger.debug( "Deleting expired scaling activities" );
    scalingActivities.deleteByCreatedAge( null, System.currentTimeMillis() - AutoScalingConfiguration.getActivityExpiryMillis() );
  }

  /**
   * Periodically executed scaling work.
   */
  private void runningInstanceChecks() {
    final Map<String,AutoScalingGroup> autoScalingAccounts = Maps.newHashMap( );
    try {
      for ( final AutoScalingGroup group : autoScalingGroups.listRequiringMonitoring( 10000L ) ) {
        autoScalingAccounts.put( group.getOwnerAccountNumber(), group );
        final List<AutoScalingInstance> groupInstances = autoScalingInstances.listByGroup( group );
        if ( !groupInstances.isEmpty() ) runTask( new MonitoringScalingProcessTask(
            group,
            Lists.newArrayList( Iterables.transform( Iterables.filter( groupInstances, LifecycleState.Pending ), instanceId( ) ) ),
            Lists.newArrayList( Iterables.transform( Iterables.filter( groupInstances, LifecycleState.InService ), instanceId() ) )
        ) );
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }

    // Terminate rogue instances
    try {
      for ( final AutoScalingGroup group : autoScalingAccounts.values() ) {
        runTask( new UntrackedInstanceTerminationScalingProcessTask( group ) );
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }
  }

  /**
   * Periodically executed scaling work.
   */
  private void submitMetrics() {
    try {
      for ( final AutoScalingGroup group : autoScalingGroups.listRequiringMonitoring( 10000L ) ) {
        if ( !group.getEnabledMetrics().isEmpty() ) {
          final List<AutoScalingInstance> groupInstances = Sets.intersection( group.getEnabledMetrics(), instanceMetrics ).isEmpty() ?
              Collections.<AutoScalingInstance>emptyList() :
              autoScalingInstances.listByGroup( group );
          runTask( new MetricsSubmissionScalingProcessTask(
              group,
              groupInstances ) );
        }
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }
  }

  /**
   * Periodically executed scaling work.
   */
  private void replaceUnhealthy() throws AutoScalingMetadataException {
    for ( final AutoScalingGroup group : autoScalingGroups.listRequiringInstanceReplacement() ) {
      runTask( perhapsReplaceInstances( group ) ) ;
    }
  }

  /**
   * Periodically executed scaling work.
   */
  private void scalingActivities() throws AutoScalingMetadataException {
    for ( final AutoScalingGroup group : autoScalingGroups.listRequiringScaling() ) {
      runTask( perhapsScale( group ) );
    }
  }

  /**
   * Periodically executed scaling work.
   */
  private void progressUnstableStates() {
    for ( final UnstableInstanceState state : unstableInstanceStates ) {
      try {
        final List<AutoScalingInstance> instanceInState =
            autoScalingInstances.listByState( state.getLifecycleState(), state.getConfigurationState() );
        final Set<String> groupArns = Sets.newHashSet( Iterables.transform( instanceInState, AutoScalingInstances.groupArn() ) );
        for ( final String groupArn : groupArns ) {
          final Iterable<AutoScalingInstance> groupInstances =
              Iterables.filter( instanceInState, CollectionUtils.propertyPredicate( groupArn, AutoScalingInstances.groupArn() ) );
          runTask( state.getStateProgressFunction().apply( groupInstances ) );
        }
      } catch ( Exception e ) {
        logger.error( e, e );
      }
    }
  }

  /**
   * Periodically executed scaling work.
   */
  private void updateUnavailableZones() throws AutoScalingMetadataException {
    final Set<String> unavailableZones = zoneMonitor.getUnavailableZones( AutoScalingConfiguration.getZoneFailureThresholdMillis() );
    zoneAvailabilityMarkers.updateUnavailableZones( unavailableZones, new ZoneCallback(){
      @Override
      public void notifyChangedZones( final Set<String> zones ) throws AutoScalingMetadataException {
        autoScalingGroups.markScalingRequiredForZones( zones );
      }
    } );
  }

  private List<String> validateReferences( final OwnerFullName owner,
                                           final Iterable<String> availabilityZones,
                                           final Iterable<String> loadBalancerNames,
                                           final Iterable<String> imageIds,
                                           @Nullable final String instanceType,
                                           @Nullable final String keyName,
                                           final Iterable<String> securityGroups,
                                           @Nullable final String iamInstanceProfile ) {
    final List<String> errors = Lists.newArrayList();

    final ValidationScalingProcessTask task = new ValidationScalingProcessTask(
        owner,
        Lists.newArrayList( Sets.newLinkedHashSet( availabilityZones ) ),
        Lists.newArrayList( Sets.newLinkedHashSet( loadBalancerNames ) ),
        Lists.newArrayList( Sets.newLinkedHashSet( imageIds ) ),
        instanceType,
        keyName,
        Lists.newArrayList( Sets.newLinkedHashSet( securityGroups ) ) );
    runTask( task );
    try {
      final boolean success = task.getFuture().get();
      if ( success ) {
        errors.addAll( task.getValidationErrors() );
      } else if ( task.shouldRun() ) {
        errors.add("Unable to validate references at this time.");
      }

      // validate IAM instance profile
      validateIamInstanceProfile( owner, iamInstanceProfile, errors );

    } catch ( ExecutionException e ) {
      logger.error( e, e );
      errors.add("Error during reference validation");
    } catch ( InterruptedException e ) {
      Thread.currentThread().interrupt();
      errors.add("Validation interrupted");
    }

    return errors;
  }

  private void validateIamInstanceProfile( final OwnerFullName owner,
                                           final String iamInstanceProfile,
                                           final List<String> errors ) {
    if ( iamInstanceProfile != null ) try {
      final String accountNumber = owner.getAccountNumber();
      String instanceProfileName = iamInstanceProfile;
      if ( iamInstanceProfile.startsWith( "arn:" )  ) {
        final Ern ern = Ern.parse( iamInstanceProfile );
        if ( ern instanceof EuareResourceName &&
            INSTANCE_PROFILE_RESOURCE.equals( ern.getResourceType() ) ) {
          if ( accountNumber.equals( ern.getNamespace() ) ) {
            instanceProfileName = ((EuareResourceName)ern).getName();
          } else {
            instanceProfileName = null;
            errors.add( "Invalid instance profile: " + iamInstanceProfile );
          }
        } else {
          instanceProfileName = null;
          errors.add( "Invalid instance profile: " + iamInstanceProfile );
        }
      }
      if ( instanceProfileName != null ) {
        Accounts.lookupAccountById( accountNumber ).lookupInstanceProfileByName( instanceProfileName );
      }
    } catch ( Exception e ) {
      errors.add( "Invalid instance profile: " + iamInstanceProfile );
    }
  }

  private boolean scalingProcessEnabled( final ScalingProcessType type, final AutoScalingGroup group ) {
    return !AutoScalingConfiguration.getSuspendedProcesses().contains( type ) && type.apply( group );
  }

  private void setScalingNotRequired( final AutoScalingGroup group ) {
    try {
      updateScalingRequiredFlag( group, false );
    } catch ( AutoScalingMetadataException e ) {
      logger.error( e, e );
    }
  }

  private void updateScalingRequiredFlag( final AutoScalingGroup group,
                                          final boolean scalingRequired ) throws AutoScalingMetadataException {
    autoScalingGroups.update(
        group.getOwner(),
        group.getAutoScalingGroupName(),
        new Callback<AutoScalingGroup>(){
          @Override
          public void fire( final AutoScalingGroup autoScalingGroup ) {
            if ( scalingRequired || group.getVersion().equals( autoScalingGroup.getVersion() ) ) {
              autoScalingGroup.setScalingRequired( scalingRequired );
              if ( !scalingRequired ) {
                autoScalingGroup.setScalingCauses( Lists.<GroupScalingCause>newArrayList() );
              }
            }
          }
        } );
  }

  private Function<Iterable<AutoScalingInstance>,TerminateInstancesScalingProcessTask> terminateInstancesTask() {
    return new Function<Iterable<AutoScalingInstance>,TerminateInstancesScalingProcessTask>(){
      @Override
      public TerminateInstancesScalingProcessTask apply( final Iterable<AutoScalingInstance> groupInstances ) {
        return terminateInstancesTask( groupInstances );
      }
    };
  }

  private TerminateInstancesScalingProcessTask terminateInstancesTask( final Iterable<AutoScalingInstance> groupInstances ) {
    return new TerminateInstancesScalingProcessTask(
        Iterables.get( groupInstances, 0 ).getAutoScalingGroup(),
        Iterables.get( groupInstances, 0 ).getAutoScalingGroup().getCapacity(),
        Lists.newArrayList( Iterables.transform( groupInstances, AutoScalingInstances.instanceId() ) ),
        Collections.<ActivityCause>emptyList(),
        false,
        true,
        true );
  }

  private ScalingProcessTask<?> perhapsTerminateInstances( final AutoScalingGroup group,
                                                           final int terminateCount ) {
    final List<String> instancesToTerminate = Lists.newArrayList();
    int currentCapacity = 0;
    try {
      final List<AutoScalingInstance> currentInstances =
          autoScalingInstances.listByGroup( group );
      currentCapacity = currentInstances.size();
      if ( currentInstances.size() == terminateCount ) {
        Iterables.addAll(
            instancesToTerminate,
            Iterables.transform( currentInstances, AutoScalingInstances.instanceId()) );
      } else {
        // First terminate instances in zones that are no longer in use
        final Set<String> groupZones = Sets.newLinkedHashSet( group.getAvailabilityZones() );
        groupZones.removeAll( zoneMonitor.getUnavailableZones( AutoScalingConfiguration.getZoneFailureThresholdMillis() ) ) ;
        final Set<String> unwantedZones = Sets.newHashSet( Iterables.transform( currentInstances, availabilityZone() ) );
        unwantedZones.removeAll( groupZones );

        final Set<String> targetZones;
        final List<AutoScalingInstance> remainingInstances = Lists.newArrayList( currentInstances );
        if ( !unwantedZones.isEmpty() ) {
          int unwantedInstanceCount = CollectionUtils.reduce(
              currentInstances, 0, CollectionUtils.count( withAvailabilityZone( unwantedZones ) ) );
          if ( unwantedInstanceCount < terminateCount ) {
            Iterable<AutoScalingInstance> unwantedInstances =
                Iterables.filter( currentInstances, withAvailabilityZone( unwantedZones ) );
            Iterables.addAll( instancesToTerminate, Iterables.transform( unwantedInstances, instanceId() ) );
            Iterables.removeAll( remainingInstances, Lists.newArrayList( unwantedInstances ) );
            targetZones = groupZones;
          } else {
            targetZones = unwantedZones;
          }
        } else {
          targetZones = groupZones;
        }

        final Map<String,Integer> zoneCounts =
            buildAvailabilityZoneInstanceCounts( currentInstances, targetZones );

        for ( int i=instancesToTerminate.size(); i<terminateCount && remainingInstances.size()>=1; i++ ) {
          final Map.Entry<String,Integer> entry = selectEntry( zoneCounts, Ordering.natural().reverse() );
          final AutoScalingInstance instanceForTermination = TerminationPolicyType.selectForTermination(
              group.getTerminationPolicies(),
              Lists.newArrayList( Iterables.filter( remainingInstances, withAvailabilityZone( entry.getKey() ) ) ) );
          remainingInstances.remove( instanceForTermination );
          entry.setValue( entry.getValue() - 1 );
          instancesToTerminate.add( instanceForTermination.getInstanceId() );
        }
      }
    } catch ( final Exception e ) {
      logger.error( e, e );
    }

    final List<ActivityCause> causes = Lists.newArrayList();
    causes.add( new ActivityCause( String.format( "an instance was taken out of service in response to a difference between desired and actual capacity, shrinking the capacity from %1$d to %2$d",
        group.getCapacity(),
        group.getCapacity() - instancesToTerminate.size() ) ) );
    for ( final String instanceId : instancesToTerminate ) {
      causes.add( new ActivityCause( String.format( "instance %1$s was selected for termination", instanceId ) ) );
    }

    return removeFromLoadBalancerOrTerminate( group, currentCapacity, instancesToTerminate, causes, false );
  }

  private ScalingProcessTask<?> perhapsReplaceInstances( final AutoScalingGroup group ) {
    final List<String> instancesToTerminate = Lists.newArrayList();
    if ( scalingProcessEnabled( ScalingProcessType.ReplaceUnhealthy, group ) ) try {
      final List<AutoScalingInstance> currentInstances =
          autoScalingInstances.listUnhealthyByGroup( group );
      Iterables.addAll(
            instancesToTerminate,
            Iterables.limit(
              Iterables.transform( currentInstances, AutoScalingInstances.instanceId()),
                Math.min( AutoScalingConfiguration.getMaxLaunchIncrement(), currentInstances.size() ) ) );
    } catch ( final Exception e ) {
      logger.error( e, e );
    }
    return removeFromLoadBalancerOrTerminate( group, group.getCapacity(), instancesToTerminate, Collections.singletonList( new ActivityCause( "an instance was taken out of service in response to a health-check" ) ), true );
  }

  private ScalingProcessTask<?> perhapsScale( final AutoScalingGroup group ) {
    final List<AutoScalingInstance> currentInstances;
    try {
      currentInstances = autoScalingInstances.listByGroup( group );
    } catch ( final Exception e ) {
      logger.error( e, e );
      return new LaunchInstancesScalingProcessTask( group, 0, "" );
    }

    if ( group.getCapacity() > group.getDesiredCapacity() ) {
      if ( !Iterables.all( currentInstances, Predicates.and( LifecycleState.InService, ConfigurationState.Registered, HealthStatus.Healthy ) ) ) {
        // Wait for terminations / launches to complete before further scaling.
        return new LaunchInstancesScalingProcessTask( group, 0, "" );
      }
      return perhapsTerminateInstances( group, group.getCapacity() - group.getDesiredCapacity() );
    } else {
      final List<String> zones =
          Lists.transform( currentInstances, AutoScalingInstances.availabilityZone() );
      final Set<String> groupZones = Sets.newLinkedHashSet( group.getAvailabilityZones() );
      final Set<String> unavailableZones = zoneMonitor.getUnavailableZones( AutoScalingConfiguration.getZoneFailureThresholdMillis() );
      groupZones.removeAll( unavailableZones );
      final int expectedInstancesPerZone = group.getCapacity() / Math.max( 1, groupZones.size() );
      int requiredInstances = 0;
      for ( final String zone : groupZones ) {
        int instanceCount = CollectionUtils.reduce( zones, 0, CollectionUtils.count( Predicates.equalTo( zone ) ) );
        if ( instanceCount < expectedInstancesPerZone ) {
          requiredInstances += expectedInstancesPerZone - instanceCount;
        }
      }

      final int hardInstanceLimit = group.getDesiredCapacity() + Math.max( 1, group.getDesiredCapacity() / 10 );
      if ( requiredInstances + group.getCapacity() > hardInstanceLimit ) {
        requiredInstances = hardInstanceLimit - group.getCapacity();
      } else if ( requiredInstances + group.getCapacity() < group.getDesiredCapacity() ) {
        requiredInstances = group.getDesiredCapacity() - group.getCapacity();
      }

      if ( requiredInstances == 0 ) {
        setScalingNotRequired( group );
      } else if ( !scalingProcessEnabled( ScalingProcessType.AZRebalance, group ) &&
          group.getCapacity().equals( group.getDesiredCapacity() ) ) {
        requiredInstances = 0; // rebalancing disabled
      }

      String cause;
      if ( group.getCapacity() < group.getDesiredCapacity() ) {
        cause = String.format( "an instance was started in response to a difference between desired and actual capacity, increasing the capacity from %1$d to %2$d",
            group.getCapacity( ),
            group.getCapacity( ) + requiredInstances );
      } else {
        final Set<String> groupZoneSet = Sets.newHashSet( group.getAvailabilityZones() );
        final Set<String> invalidZoneSet = Sets.newTreeSet();
        Iterables.addAll( invalidZoneSet, Sets.intersection( groupZoneSet, unavailableZones ) );
        Iterables.addAll( invalidZoneSet, Sets.difference( Sets.newHashSet( zones ), groupZoneSet ) );
        final List<Integer> invalidZoneCounts = Lists.newArrayList();
        for ( final String zone : invalidZoneSet ) {
          invalidZoneCounts.add( CollectionUtils.reduce( zones, 0, CollectionUtils.count( Predicates.equalTo( zone ) ) ) );
        }
        final String invalidZones = Joiner.on( ", " ).join( invalidZoneSet );
        final String invalidZoneInstanceCounts = Joiner.on( ", " ).join( invalidZoneCounts );
        cause = String.format( "invalid availability zones %1$s had %2$s instances respectively. An instance was launched to aid in migrating instances from these zones to valid ones",
            invalidZones,
            invalidZoneInstanceCounts );
      }

      return new LaunchInstancesScalingProcessTask( group, requiredInstances, cause );
    }
  }

  private Function<Iterable<AutoScalingInstance>,AddToLoadBalancerScalingProcessTask> addToLoadBalancer() {
    return new Function<Iterable<AutoScalingInstance>,AddToLoadBalancerScalingProcessTask>(){
      @Override
      public AddToLoadBalancerScalingProcessTask apply( final Iterable<AutoScalingInstance> groupInstances ) {
        return addToLoadBalancer( groupInstances );
      }
    };
  }

  private AddToLoadBalancerScalingProcessTask addToLoadBalancer( final Iterable<AutoScalingInstance> unregisteredInstances ) {
    final AutoScalingGroup group = Iterables.get( unregisteredInstances, 0 ).getAutoScalingGroup();
    final List<String> instancesToRegister = Lists.newArrayList();
    if ( group.getLoadBalancerNames().isEmpty() || !scalingProcessEnabled( ScalingProcessType.AddToLoadBalancer, group ) ) {
      // nothing to do, mark instances as registered
      transitionToRegistered(
          group,
          Lists.newArrayList( Iterables.transform( unregisteredInstances, AutoScalingInstances.instanceId() ) ) );
    } else {
      Iterables.addAll(
          instancesToRegister,
          Iterables.transform( unregisteredInstances, AutoScalingInstances.instanceId() ) );
    }

    return new AddToLoadBalancerScalingProcessTask( group, instancesToRegister );
  }

  private Function<Iterable<AutoScalingInstance>,ScalingProcessTask<?>> removeFromLoadBalancerOrTerminate() {
    return new Function<Iterable<AutoScalingInstance>,ScalingProcessTask<?>>(){
      @Override
      public ScalingProcessTask<?> apply( final Iterable<AutoScalingInstance> groupInstances ) {
        return removeFromLoadBalancerOrTerminate( Iterables.get( groupInstances, 0 ).getAutoScalingGroup(),
            Lists.newArrayList( Iterables.transform( groupInstances, AutoScalingInstances.instanceId() ) ) );
      }
    };
  }

  private ScalingProcessTask<?> removeFromLoadBalancerOrTerminate( final AutoScalingGroup group,
                                                                   final List<String> registeredInstances ) {
    return removeFromLoadBalancerOrTerminate( group, group.getCapacity(), registeredInstances, Collections.<ActivityCause>emptyList(), false );
  }

  private ScalingProcessTask<?> removeFromLoadBalancerOrTerminate( final AutoScalingGroup group,
                                                                   final int currentCapacity,
                                                                   final List<String> registeredInstances,
                                                                   final List<ActivityCause> causes,
                                                                   final boolean replace ) {
    final ScalingProcessTask<?> task;
    if ( group.getLoadBalancerNames().isEmpty() ) {
      // deregistration not required, mark instances
      transitionToDeregistered( group, registeredInstances );
      task = new TerminateInstancesScalingProcessTask( group, currentCapacity, registeredInstances, causes, replace, true, true );
    } else {
      task = new RemoveFromLoadBalancerScalingProcessTask( group, currentCapacity, registeredInstances, causes, replace );
    }

    return task;
  }

  private RunInstancesType runInstances( final AutoScalingGroup group,
                                         final String availabilityZone,
                                         final String clientToken,
                                         final int attemptToLaunch ) {
    final LaunchConfiguration launchConfiguration = group.getLaunchConfiguration();
    final RunInstancesType runInstances = TypeMappers.transform( launchConfiguration, RunInstancesType.class );
    runInstances.setAvailabilityZone( availabilityZone );
    runInstances.setClientToken( clientToken );
    runInstances.setMaxCount( attemptToLaunch );
    return runInstances;
  }

  private CreateTagsType tagInstances( final List<String> instanceIds,
                                       final String autoScalingGroupName,
                                       final List<Tag> tags ) {
    final CreateTagsType createTags = new CreateTagsType();
    createTags.getTagSet().add( new ResourceTag( "aws:autoscaling:groupName", autoScalingGroupName ) );
    for ( final Tag tag : tags ) {
      createTags.getTagSet().add( new ResourceTag( tag.getKey(), tag.getValue() ) );
    }
    createTags.getResourcesSet().addAll( instanceIds );
    return createTags;
  }

  private RegisterInstancesWithLoadBalancerType registerInstances( final String loadBalancerName,
                                                                   final List<String> instanceIds ) {
    return new RegisterInstancesWithLoadBalancerType( loadBalancerName, instanceIds );
  }

  private DeregisterInstancesFromLoadBalancerType deregisterInstances( final String loadBalancerName,
                                                                       final List<String> instanceIds ) {
    return new DeregisterInstancesFromLoadBalancerType( loadBalancerName, instanceIds );
  }

  private DescribeInstanceHealthType describeInstanceHealth( final String loadBalancerName ) {
    return new DescribeInstanceHealthType( loadBalancerName, Collections.<String>emptyList() );
  }

  private TerminateInstancesType terminateInstances( final Collection<String> instancesToTerminate ) {
    final TerminateInstancesType terminateInstances = new TerminateInstancesType();
    terminateInstances.getInstancesSet().addAll( instancesToTerminate );
    return terminateInstances;
  }

  private DescribeInstanceStatusType monitorInstances( final Collection<String> instanceIds ) {
    final DescribeInstanceStatusType describeInstanceStatusType = new DescribeInstanceStatusType();
    describeInstanceStatusType.getInstancesSet().addAll( instanceIds );
    describeInstanceStatusType.getFilterSet().add( filter( "system-status.status", "ok" ) );
    describeInstanceStatusType.getFilterSet().add( filter( "instance-status.status", "ok" ) );
    return describeInstanceStatusType;
  }

  private DescribeTagsType describeTags() {
    final DescribeTagsType describeTagsType = new DescribeTagsType();
    describeTagsType.getFilterSet().add( filter( "key", "aws:autoscaling:groupName" ) );
    describeTagsType.getFilterSet().add( filter( "resource-type", "instance" ) );
    return describeTagsType;
  }

  private Filter filter( final String name, final String value ) {
    final Filter filter = new Filter();
    filter.setName( name );
    filter.getValueSet().add( value );
    return filter;
  }

  private Filter filter( final String name, final Collection<String> values ) {
    final Filter filter = new Filter();
    filter.setName( name );
    filter.getValueSet().addAll( values );
    return filter;
  }

  private void transitionToRegistered( final AutoScalingGroup group, final List<String> instanceIds ) {
    try {
      autoScalingInstances.transitionConfigurationState(
          group,
          ConfigurationState.Instantiated,
          ConfigurationState.Registered,
          instanceIds );
    } catch ( AutoScalingMetadataException e ) {
      logger.error( e, e );
    }
  }

  private void transitionToDeregistered( final AutoScalingGroup group, final List<String> instanceIds ) {
    try {
      autoScalingInstances.transitionConfigurationState(
          group,
          ConfigurationState.Registered,
          ConfigurationState.Instantiated,
          instanceIds );
    } catch ( AutoScalingMetadataException e ) {
      logger.error( e, e );
    }
  }

  private boolean isTimedOut( final Date timestamp ) {
    return ( timestamp() - timestamp.getTime() ) > AutoScalingConfiguration.getActivityTimeoutMillis();
  }

  private Map<String,Integer> buildAvailabilityZoneInstanceCounts( final Collection<AutoScalingInstance> instances,
                                                                   final Collection<String> availabilityZones ) {
    final Map<String,Integer> instanceCountByAz = Maps.newTreeMap();
    for ( final String az : availabilityZones ) {
      instanceCountByAz.put( az,
          CollectionUtils.reduce( instances, 0,
              CollectionUtils.count( withAvailabilityZone( az ) ) ) );
    }
    return instanceCountByAz;
  }

  private Predicate<AutoScalingInstance> withAvailabilityZone( final String availabilityZone ) {
    return withAvailabilityZone( Collections.singleton( availabilityZone ) );
  }

  private Predicate<AutoScalingInstance> withAvailabilityZone( final Collection<String> availabilityZones ) {
    return Predicates.compose(
        Predicates.in( availabilityZones ),
        availabilityZone() );
  }

  private <K,V> Map.Entry<K,V> selectEntry( final Map<K,V> map, final Comparator<? super V> valueComparator ) {
    Map.Entry<K,V> entry = null;
    for ( final Map.Entry<K,V> currentEntry : map.entrySet() ) {
      if ( entry == null || valueComparator.compare( entry.getValue(), currentEntry.getValue() ) > 0) {
        entry = currentEntry;
      }
    }
    return entry;
  }

  void runTask( final ScalingProcessTask task ) {
    runner.runTask( task );
  }

  boolean taskInProgress( final String groupArn ) {
    return runner.taskInProgress( groupArn );
  }

  EucalyptusClient createEucalyptusClientForUser( final String userId ) {
    try {
      final EucalyptusClient client = new EucalyptusClient( userId );
      client.init();
      return client;
    } catch ( DispatchingClient.DispatchingClientException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  ElbClient createElbClientForUser( final String userId ) {
    try {
      final ElbClient client = new ElbClient( userId );
      client.init();
      return client;
    } catch ( DispatchingClient.DispatchingClientException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  public CloudWatchClient createCloudWatchClientForUser( final String userId ) {
    try {
      final CloudWatchClient client = new CloudWatchClient( userId );
      client.init();
      return client;
    } catch ( DispatchingClient.DispatchingClientException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  VmTypesClient createVmTypesClientForUser( final String userId ) {
    try {
      final VmTypesClient client = new VmTypesClient( userId );
      client.init();
      return client;
    } catch ( DispatchingClient.DispatchingClientException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  Supplier<String> userIdSupplier( final String accountNumber ) {
    return new Supplier<String>(){
      @Override
      public String get() {
        try {
          return Accounts.lookupAccountById( accountNumber )
              .lookupAdmin().getUserId();
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    };
  }

  List<Tag> getTags( final AutoScalingGroup group ) {
    final AccountFullName accountFullName =
        AccountFullName.getInstance( group.getOwner().getAccountNumber() );
    return TagSupport.forResourceClass( AutoScalingGroup.class ).getResourceTags(
        accountFullName,
        group.getAutoScalingGroupName(),
        new Predicate<Tag>(){
          @Override
          public boolean apply( final Tag tag ) {
            return Objects.firstNonNull( tag.getPropagateAtLaunch(), Boolean.FALSE );
          }
        } );
  }

  private boolean shouldSuspendDueToLaunchFailure( final AutoScalingGroup group ) {
    while ( true ) {
      final Integer count = launchFailureCounters.get( group.getArn() );
      final Integer newCount = Objects.firstNonNull( count, 0 ) + 1;
      if ( ( count == null && launchFailureCounters.putIfAbsent( group.getArn(), newCount ) == null ) ||
           ( count != null && launchFailureCounters.replace( group.getArn(), count, newCount ) ) ) {
        return newCount >= AutoScalingConfiguration.getSuspensionLaunchAttemptsThreshold();
      }
    }
  }

  private void clearLaunchFailures( final AutoScalingGroup group ) {
    launchFailureCounters.remove( group.getArn() );
  }

  private interface ActivityContext {
    String getUserId();
    EucalyptusClient getEucalyptusClient();
    ElbClient getElbClient();
    CloudWatchClient getCloudWatchClient();
    VmTypesClient getVmTypesClient();
  }

  private abstract class ScalingActivityTask<RES extends BaseMessage> {
    private volatile ScalingActivity activity;
    private final boolean persist;

    protected ScalingActivityTask( final ScalingActivity activity ) {
      this( activity, true );
    }

    protected ScalingActivityTask( final ScalingActivity activity,
                                   final boolean persist ) {
      this.activity = activity;
      this.persist = persist;
    }

    ScalingActivity getActivity() {
      return activity;
    }

    AutoScalingGroup getGroup() {
      return getActivity().getGroup();
    }

    OwnerFullName getOwner() {
      return getGroup().getOwner();
    }

    final CheckedListenableFuture<Boolean> dispatch( final ActivityContext context ) {
      try {
        activity = persist ? scalingActivities.save( activity ) : activity;
        final CheckedListenableFuture<Boolean> future = Futures.newGenericeFuture();
        dispatchInternal( context, new Callback.Checked<RES>(){
          @Override
          public void fireException( final Throwable throwable ) {
            try {
              dispatchFailure( context, throwable );
            } finally {
              future.set( false );
            }
          }

          @Override
          public void fire( final RES response ) {
            try {
              dispatchSuccess( context, response );
            } finally {
              future.set( true );
            }
          }
        } );
        return future;
      } catch ( Throwable e ) {
        dispatchFailure( context, e );
        logger.error( e, e );
      }
      return Futures.predestinedFuture( false );
    }

    abstract void dispatchInternal( ActivityContext context, Callback.Checked<RES> callback );

    void dispatchFailure( ActivityContext context, Throwable throwable ) {
      // error, assume no instances run for now
      Logs.extreme().error( "Activity error", throwable );
      setActivityFinalStatus( ActivityStatusCode.Failed, throwable.getMessage(), null );
    }

    abstract void dispatchSuccess( ActivityContext context, RES response );

    void setActivityStatus( final ActivityStatusCode activityStatusCode,
                            final int progress ) {
      updateActivity( new Callback<ScalingActivity>( ) {
        @Override
        public void fire( final ScalingActivity input ) {
          input.setStatusCode( activityStatusCode );
          input.setProgress( progress );
        }
      } );
    }

    void setActivityFinalStatus( final ActivityStatusCode activityStatusCode ) {
      setActivityFinalStatus( activityStatusCode, null, null );
    }

    void setActivityFinalStatus( @Nonnull  final ActivityStatusCode activityStatusCode,
                                 @Nullable final String message,
                                 @Nullable final String description ) {
      updateActivity( new Callback<ScalingActivity>( ) {
        @Override
        public void fire( final ScalingActivity input ) {
          input.setStatusCode( activityStatusCode );
          input.setStatusMessage( message );
          if ( description != null ) input.setDescription( description );
          input.setProgress( 100 );
          input.setEndTime( new Date() );
        }
      } );
    }

    void updateActivity( @Nonnull  final Callback<ScalingActivity> callback ) {
      final ScalingActivity activity = getActivity();
      if ( activity.getCreationTimestamp() != null ) { // only update if persistent
        try {
          scalingActivities.update(
              activity.getOwner(),
              activity.getActivityId(),
              callback );
        } catch ( AutoScalingMetadataNotFoundException e ) {
          // this is expected when terminating instances and deleting the group
          Logs.exhaust().debug( e, e );
        } catch ( AutoScalingMetadataException e ) {
          logger.error( e, e );
        }
      }
    }
  }

  abstract class ScalingProcessTask<AT extends ScalingActivityTask> extends TaskWithBackOff implements ActivityContext {
    private final AutoScalingGroup group;
    private final Supplier<String> userIdSupplier;
    private final AtomicReference<List<ScalingActivity>> activities =
        new AtomicReference<List<ScalingActivity>>( Collections.<ScalingActivity>emptyList() );
    private volatile CheckedListenableFuture<Boolean> taskFuture;

    ScalingProcessTask( final String uniqueKey,
                        final AutoScalingGroup group,
                        final String activity ) {
      super( uniqueKey, activity );
      this.group = group;
      this.userIdSupplier = Suppliers.memoize( userIdSupplier( group.getOwnerAccountNumber() ) );
    }

    ScalingProcessTask( final AutoScalingGroup group,
                        final String activity ) {
      this( group.getArn(), group, activity  );
    }

    List<ScalingActivity> getActivities() {
      return activities.get();
    }

    AutoScalingGroup getGroup() {
      return group;
    }

    OwnerFullName getOwner() {
      return getGroup().getOwner();
    }

    @Override
    public String getUserId() {
      return userIdSupplier.get();
    }

    @Override
    public EucalyptusClient getEucalyptusClient() {
      return createEucalyptusClientForUser( getUserId() );
    }

    @Override
    public ElbClient getElbClient() {
      return createElbClientForUser( getUserId() );
    }

    @Override
    public CloudWatchClient getCloudWatchClient() {
      return createCloudWatchClientForUser( getUserId() );
    }

    @Override
    public VmTypesClient getVmTypesClient() {
      return createVmTypesClientForUser( getUserId() );
    }

    final ActivityCause cause( final String cause ) {
      return new ActivityCause( new Date(timestamp()), cause );
    }

    ScalingActivity newActivity() {
      return newActivity( null, 0, null, Collections.<ActivityCause>emptyList(), null );
    }

    ScalingActivity newActivity( @Nullable final String description,
                                           final int progress,
                                 @Nullable final String clientToken,
                                 @Nonnull  final List<ActivityCause> activityCauses,
                                 @Nullable final ActivityStatusCode activityStatusCode ) {
      final List<ActivityCause> causes = Lists.newArrayList();
      Iterables.addAll( causes, Iterables.transform( group.getScalingCauses(), CauseTransform.INSTANCE ) );
      Iterables.addAll( causes, activityCauses );
      final ScalingActivity scalingActivity = ScalingActivity.create( group, clientToken, causes );
      if ( description != null ) {
        scalingActivity.setDescription( description );
      }
      scalingActivity.setProgress( progress );
      if ( activityStatusCode != null ) {
        scalingActivity.setStatusCode( activityStatusCode );
      }
      return scalingActivity;
    }

    abstract boolean shouldRun();
    abstract List<AT> buildActivityTasks() throws AutoScalingMetadataException;

    @Override
    ScalingProcessTask onSuccess() {
      return null;
    }

    void partialSuccess( final List<AT> tasks ) {
    }

    void failure( final List<AT> tasks ) {
    }

    Future<Boolean> getFuture() {
      Future<Boolean> future = taskFuture;
      if ( future == null ) {
        future = Futures.predestinedFuture( false );
      }
      return future;
    }

    @Override
    void runTask() {
      if ( !shouldRun() ) {
        success();
        return;
      }

      final List<CheckedListenableFuture<Boolean>> dispatchFutures = Lists.newArrayList();
      final List<AT> activities = Lists.newArrayList();
      final List<ScalingActivity> scalingActivities = Lists.newArrayList();
      try {
        activities.addAll( buildActivityTasks() );
        for ( final ScalingActivityTask<?> activity : activities ) {
          dispatchFutures.add( activity.dispatch( this ) );
          scalingActivities.add( activity.getActivity() );
        }
        this.activities.set( ImmutableList.copyOf( scalingActivities ) );
      } catch ( final Exception e ) {
        logger.error( e, e );
      } finally {
        if ( dispatchFutures.isEmpty() ) {
          failure();
        } else {
          taskFuture = Futures.newGenericeFuture();
          final CheckedListenableFuture<List<Boolean>> resultFuture = Futures.allAsList( dispatchFutures );
          resultFuture.addListener( new Runnable() {
            @Override
            public void run() {
              boolean success = false;
              try {
                success = resultFuture.get().contains( true );
              } catch ( Exception e ) {
                logger.error( e, e );
              }
              if ( success ) {
                partialSuccess( activities );
                success();
                taskFuture.set( true );
              } else {
                failure( activities );
                failure();
                taskFuture.set( false );
              }
            }
          } );
        }
      }
    }
  }

  private class LaunchInstanceScalingActivityTask extends ScalingActivityTask<RunInstancesResponseType> {
    private final String availabilityZone;
    private final String clientToken;
    private final AtomicReference<List<String>> instanceIds = new AtomicReference<List<String>>(
        Collections.<String>emptyList()
    );

    private LaunchInstanceScalingActivityTask( final ScalingActivity activity,
                                               final String availabilityZone,
                                               final String clientToken ) {
      super( activity );
      this.availabilityZone = availabilityZone;
      this.clientToken = clientToken;
    }

    @Override
    void dispatchInternal( final ActivityContext context,
                           final Callback.Checked<RunInstancesResponseType> callback ) {
      setActivityStatus( ActivityStatusCode.InProgress, 50 );
      final EucalyptusClient client = context.getEucalyptusClient();
      client.dispatch( runInstances( getGroup(), availabilityZone, clientToken, 1 ), callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final RunInstancesResponseType response ) {
      final List<String> instanceIds = Lists.newArrayList();
      for ( final RunningInstancesItemType item : response.getRsvInfo().getInstancesSet() ) {
        instanceIds.add( item.getInstanceId() );
        final AutoScalingInstance instance = AutoScalingInstance.create(
            getOwner(),
            item.getInstanceId(),
            item.getPlacement(),
            getGroup() );
        try {
          autoScalingInstances.save( instance );
        } catch ( AutoScalingMetadataException e ) {
          logger.error( e, e );
        }
      }

      this.instanceIds.set( ImmutableList.copyOf( instanceIds ) );

      setActivityFinalStatus( ActivityStatusCode.Successful, null, String.format( "Launching a new EC2 instance: %1$s", Joiner.on(", ").join(instanceIds) ) );
    }

    List<String> getInstanceIds() {
      return instanceIds.get();
    }
  }

  private class LaunchInstancesScalingProcessTask extends ScalingProcessTask<LaunchInstanceScalingActivityTask> {
    private final int launchCount;
    private final String cause;

    LaunchInstancesScalingProcessTask( final AutoScalingGroup group,
                                       final int launchCount,
                                       final String cause ) {
      super( group, "Launch" );
      this.launchCount = launchCount;
      this.cause = cause;
    }

    @Override
    boolean shouldRun() {
      return launchCount > 0 && scalingProcessEnabled( ScalingProcessType.Launch, getGroup() );
    }

    @Override
    List<LaunchInstanceScalingActivityTask> buildActivityTasks() throws AutoScalingMetadataException {
      final List<AutoScalingInstance> instances = autoScalingInstances.listByGroup( getGroup( ) );
      final Set<String> zonesToUse = Sets.newHashSet( getGroup().getAvailabilityZones() );
      zonesToUse.removeAll( zoneMonitor.getUnavailableZones( AutoScalingConfiguration.getZoneFailureThresholdMillis() ) );
      final Map<String,Integer> zoneCounts =
          buildAvailabilityZoneInstanceCounts( instances, zonesToUse );
      final int attemptToLaunch = Math.min( AutoScalingConfiguration.getMaxLaunchIncrement(), launchCount );
      final List<LaunchInstanceScalingActivityTask> activities = Lists.newArrayList();
      for ( int i=0; i<attemptToLaunch; i++ ) {
        final Map.Entry<String,Integer> entry = selectEntry( zoneCounts, Ordering.natural() );
        if ( entry != null ) {
          final String zone = entry.getKey();
          final String clientToken = String.format( "%1$s_%2$s_1",
              UUID.randomUUID().toString(),
              Iterables.getFirst( Splitter.fixedLength( 24 ).split( zone ), "" ) );
          entry.setValue( entry.getValue() + 1 );
          activities.add( new LaunchInstanceScalingActivityTask( newActivity("Launching a new EC2 instance", 30, clientToken, Lists.newArrayList( cause( cause ) ), ActivityStatusCode.PreInService), zone, clientToken ) );
        }
      }
      return activities;
    }

    @Override
    void failure( final List<LaunchInstanceScalingActivityTask> tasks ) {
      // Check to see if we should suspend activities for this group
      // - Group zones must not be unavailable
      // - Group must have been trying to launch instances for X period (unchanged)
      if ( !zoneMonitor.getUnavailableZones( 0 ).removeAll( getGroup().getAvailabilityZones() ) &&
          (getGroup().getLastUpdateTimestamp().getTime() + AutoScalingConfiguration.getSuspensionTimeoutMillis() ) < timestamp() ) {
        if ( shouldSuspendDueToLaunchFailure( getGroup() ) ) try {
          autoScalingGroups.update(
              getOwner(),
              getGroup().getAutoScalingGroupName(),
              new Callback<AutoScalingGroup>() {
                @Override
                public void fire( final AutoScalingGroup autoScalingGroup ) {
                  autoScalingGroup.getSuspendedProcesses().add(
                      SuspendedProcess.createAdministrative( ScalingProcessType.Launch ) );
                }
              } );
        } catch ( AutoScalingMetadataException e ) {
          logger.error( e, e );
        }
      } else {
        clearLaunchFailures( getGroup() );
      }
    }

    @Override
    void partialSuccess( final List<LaunchInstanceScalingActivityTask> tasks ) {
      clearLaunchFailures( getGroup() );

      final List<String> instanceIds = Lists.newArrayList();
      for ( final LaunchInstanceScalingActivityTask task : tasks ) {
        instanceIds.addAll( task.getInstanceIds() );
      }

      try {
        autoScalingGroups.update( getOwner(), getGroup().getAutoScalingGroupName(), new Callback<AutoScalingGroup>(){
          @Override
          public void fire( final AutoScalingGroup autoScalingGroup ) {
            autoScalingGroup.setCapacity( autoScalingGroup.getCapacity() + instanceIds.size() );
          }
        } );
      } catch ( AutoScalingMetadataException e ) {
        logger.error( e, e );
      }

      getEucalyptusClient().dispatch(
          tagInstances(
              instanceIds,
              getGroup().getAutoScalingGroupName(),
              getTags( getGroup() ) ),
          new Callback.Failure<CreateTagsResponseType>() {
            @Override
            public void fireException( final Throwable e ) {
              logger.error( e, e );
            }
          }
      );
    }
  }

  private class AddToLoadBalancerScalingActivityTask extends ScalingActivityTask<RegisterInstancesWithLoadBalancerResponseType> {
    private final String loadBalancerName;
    private final List<String> instanceIds;
    private volatile boolean registered = false;

    private AddToLoadBalancerScalingActivityTask( final ScalingActivity activity,
                                                  final String loadBalancerName,
                                                  final List<String> instanceIds ) {
      super( activity );
      this.loadBalancerName = loadBalancerName;
      this.instanceIds = instanceIds;
    }

    @Override
    void dispatchInternal( final ActivityContext context, final Callback.Checked<RegisterInstancesWithLoadBalancerResponseType> callback ) {
      final ElbClient client = context.getElbClient( );
      client.dispatch( registerInstances( loadBalancerName, instanceIds ), callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final RegisterInstancesWithLoadBalancerResponseType response ) {
      if ( response.getRegisterInstancesWithLoadBalancerResult() != null &&
          response.getRegisterInstancesWithLoadBalancerResult().getInstances() != null &&
          response.getRegisterInstancesWithLoadBalancerResult().getInstances().getMember() != null) {
        final Set<String> registeredInstances = Sets.newHashSet();
        for ( final Instance instance : response.getRegisterInstancesWithLoadBalancerResult().getInstances().getMember() ) {
          if ( instance.getInstanceId() != null ) registeredInstances.add( instance.getInstanceId() );
        }
        if ( registeredInstances.containsAll( instanceIds ) ) {
          registered = true;
        }
      }
      setActivityFinalStatus( registered ? ActivityStatusCode.Successful : ActivityStatusCode.Failed );
    }

    boolean instancesRegistered() {
      return registered;
    }
  }

  private class AddToLoadBalancerScalingProcessTask extends ScalingProcessTask<AddToLoadBalancerScalingActivityTask> {
    private final List<String> instanceIds;


    AddToLoadBalancerScalingProcessTask( final AutoScalingGroup group,
                                         final List<String> instanceIds ) {
      super( group, "AddToLoadBalancer" );
      this.instanceIds = instanceIds;
    }

    @Override
    boolean shouldRun() {
      return !instanceIds.isEmpty() &&
          !getGroup().getLoadBalancerNames().isEmpty() &&
          scalingProcessEnabled( ScalingProcessType.AddToLoadBalancer, getGroup() );
    }

    @Override
    List<AddToLoadBalancerScalingActivityTask> buildActivityTasks() throws AutoScalingMetadataException {
      final List<AddToLoadBalancerScalingActivityTask> activities = Lists.newArrayList();
      for ( final String loadBalancerName : getGroup().getLoadBalancerNames() ) {
        activities.add( new AddToLoadBalancerScalingActivityTask(
            newActivity(),
            loadBalancerName,
            instanceIds ) );
      }
      return activities;
    }

    @Override
    void failure( final List<AddToLoadBalancerScalingActivityTask> tasks ) {
      handleFailure( );
    }

    @Override
    void partialSuccess( final List<AddToLoadBalancerScalingActivityTask> tasks ) {
      boolean success = true;
      for ( AddToLoadBalancerScalingActivityTask task : tasks ) {
        success = success && task.instancesRegistered();
      }
      if ( success ) {
        transitionToRegistered( getGroup(), instanceIds );
      } else {
        handleFailure();
      }
    }

    private void handleFailure() {
      try {
        int failureCount = autoScalingInstances.registrationFailure( getGroup(), instanceIds );
        if ( failureCount > AutoScalingConfiguration.getMaxRegistrationRetries() ) {
          updateScalingRequiredFlag( getGroup(), true );
          autoScalingInstances.transitionState( getGroup(), LifecycleState.InService, LifecycleState.Terminating, instanceIds );
        }
      } catch ( final AutoScalingMetadataException e ) {
        logger.error( e, e );
      }
    }
  }

  private class RemoveFromLoadBalancerScalingActivityTask extends ScalingActivityTask<DeregisterInstancesFromLoadBalancerResponseType> {
    private final String loadBalancerName;
    private final List<String> instanceIds;
    private volatile boolean deregistered = false;

    private RemoveFromLoadBalancerScalingActivityTask( final ScalingActivity activity,
                                                       final String loadBalancerName,
                                                       final List<String> instanceIds ) {
      super( activity );
      this.loadBalancerName = loadBalancerName;
      this.instanceIds = instanceIds;
    }

    @Override
    void dispatchInternal( final ActivityContext context, final Callback.Checked<DeregisterInstancesFromLoadBalancerResponseType> callback ) {
      final ElbClient client = context.getElbClient();
      client.dispatch( deregisterInstances( loadBalancerName, instanceIds ), callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final DeregisterInstancesFromLoadBalancerResponseType response ) {
      final Set<String> registeredInstances = Sets.newHashSet();
      if ( response.getDeregisterInstancesFromLoadBalancerResult() != null &&
          response.getDeregisterInstancesFromLoadBalancerResult().getInstances() != null &&
          response.getDeregisterInstancesFromLoadBalancerResult().getInstances().getMember() != null) {
        for ( final Instance instance : response.getDeregisterInstancesFromLoadBalancerResult().getInstances().getMember() ) {
          if ( instance.getInstanceId() != null ) registeredInstances.add( instance.getInstanceId() );
        }
      }
      if ( !registeredInstances.removeAll( instanceIds ) ) {
        deregistered = true;
      }
      setActivityFinalStatus( deregistered ? ActivityStatusCode.Successful : ActivityStatusCode.Failed );
    }

    boolean instancesDeregistered() {
      return deregistered;
    }
  }

  private class RemoveFromLoadBalancerScalingProcessTask extends ScalingProcessTask<RemoveFromLoadBalancerScalingActivityTask> {
    private final List<String> instanceIds;
    private final List<ActivityCause> causes;
    private final int currentCapacity;
    private final boolean replace;
    private boolean removed = false;

    RemoveFromLoadBalancerScalingProcessTask( final AutoScalingGroup group,
                                              final int currentCapacity,
                                              final List<String> instanceIds,
                                              final List<ActivityCause> causes,
                                              final boolean replace ) {
      super( group, "RemoveFromLoadBalancer" );
      this.instanceIds = instanceIds;
      this.causes = causes;
      this.currentCapacity = currentCapacity;
      this.replace = replace;
    }

    RemoveFromLoadBalancerScalingProcessTask( final String uniqueKey,
                                              final AutoScalingGroup group,
                                              final String activity,
                                              final List<String> instanceIds,
                                              final List<ActivityCause> causes ) {
      super( uniqueKey, group, activity );
      this.instanceIds = instanceIds;
      this.causes = causes;
      this.currentCapacity = 0;
      this.replace = false;
    }

    @Override
    boolean shouldRun() {
      return !instanceIds.isEmpty() && !getGroup().getLoadBalancerNames().isEmpty();
    }

    @Override
    ScalingProcessTask onSuccess() {
      return removed ?
        new TerminateInstancesScalingProcessTask( getGroup(), currentCapacity, instanceIds, causes, replace, true, true ):
        null;
    }

    @Override
    List<RemoveFromLoadBalancerScalingActivityTask> buildActivityTasks() throws AutoScalingMetadataException {
      final List<RemoveFromLoadBalancerScalingActivityTask> activities = Lists.newArrayList();

      try {
        autoScalingInstances.transitionState( getGroup(), LifecycleState.InService, LifecycleState.Terminating, instanceIds );

        for ( final String loadBalancerName : getGroup().getLoadBalancerNames() ) {
          activities.add( new RemoveFromLoadBalancerScalingActivityTask(
              newActivity(),
              loadBalancerName,
              instanceIds ) );
         }
      } catch ( Exception e ) {
        logger.error( e, e );
      }
      return activities;
    }

    @Override
    void failure( final List<RemoveFromLoadBalancerScalingActivityTask> tasks ) {
      handleFailure( );
    }

    @Override
    void partialSuccess( final List<RemoveFromLoadBalancerScalingActivityTask> tasks ) {
      boolean success = true;
      for ( RemoveFromLoadBalancerScalingActivityTask task : tasks ) {
        success = success && task.instancesDeregistered();
      }
      if ( success ) {
        transitionToDeregistered( getGroup(), instanceIds );
        removed = true;
      } else {
        handleFailure();
      }
    }

    private void handleFailure() {
      try {
        int failureCount = autoScalingInstances.registrationFailure( getGroup(), instanceIds );
        if ( failureCount > AutoScalingConfiguration.getMaxRegistrationRetries() ) {
          transitionToDeregistered( getGroup(), instanceIds );
        }
      } catch ( final AutoScalingMetadataException e ) {
        logger.error( e, e );
      }
    }
  }

  private class TerminateInstanceScalingActivityTask extends ScalingActivityTask<TerminateInstancesResponseType> {
    private final String instanceId;
    private volatile boolean terminated = false;

    private TerminateInstanceScalingActivityTask( final ScalingActivity activity,
                                                  final boolean persist,
                                                  final String instanceId ) {
      super( activity, persist );
      this.instanceId = instanceId;
    }

    @Override
    void dispatchInternal( final ActivityContext context,
                           final Callback.Checked<TerminateInstancesResponseType> callback ) {
      final EucalyptusClient client = context.getEucalyptusClient();
      client.dispatch( terminateInstances( Collections.singleton( instanceId ) ), callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final TerminateInstancesResponseType response ) {
      try {
        // We ignore the response since we only requested termination of a
        // single instance. The response would be empty if the instance was
        // already terminated.
        final AutoScalingInstance instance = autoScalingInstances.lookup(
            getOwner(),
            instanceId );
        autoScalingInstances.delete( instance );
        terminated = true;
      } catch ( AutoScalingMetadataNotFoundException e ) {
        // no need to delete it then
        terminated = true;
      } catch ( AutoScalingMetadataException e ) {
        logger.error( e, e );
      }
      setActivityFinalStatus( terminated ?
          ActivityStatusCode.Successful :
          ActivityStatusCode.Failed
      );
    }

    boolean wasTerminated() {
      return terminated;
    }
  }

  private abstract class TerminateInstancesScalingProcessTaskSupport extends ScalingProcessTask<TerminateInstanceScalingActivityTask> {
    private final List<String> instanceIds;
    private final List<ActivityCause> causes;
    private final boolean persist;
    private final boolean scaling;
    private volatile int terminatedCount;

    TerminateInstancesScalingProcessTaskSupport( final AutoScalingGroup group,
                                                 final String activity,
                                                 final List<String> instanceIds,
                                                 final List<ActivityCause> causes,
                                                 final boolean persist,
                                                 final boolean scaling ) {
      super( group, activity );
      this.instanceIds = instanceIds;
      this.causes = causes;
      this.persist = persist;
      this.scaling = scaling;
    }

    @Override
    boolean shouldRun() {
      return !instanceIds.isEmpty() && (scalingProcessEnabled( ScalingProcessType.Terminate, getGroup() ) || !scaling);
    }

    int getTerminatedCount() {
      return terminatedCount;
    }

    int getCurrentCapacity() {
      return getGroup().getCapacity();
    }

    @Override
    List<TerminateInstanceScalingActivityTask> buildActivityTasks() {
      final List<TerminateInstanceScalingActivityTask> activities = Lists.newArrayList();

      try {
        autoScalingInstances.transitionState( getGroup(), LifecycleState.InService, LifecycleState.Terminating, instanceIds );

        for ( final String instanceId : instanceIds ) {
          activities.add( new TerminateInstanceScalingActivityTask(
              newActivity("Terminating EC2 instance: " + instanceId, 50, null, causes, ActivityStatusCode.InProgress),
              persist,
              instanceId ) );
        }
      } catch ( final AutoScalingMetadataException e ) {
        logger.error( e, e );
      }

      return activities;
    }

    @Override
    void partialSuccess( final List<TerminateInstanceScalingActivityTask> tasks ) {
      int terminatedCount = 0;
      for ( final TerminateInstanceScalingActivityTask task : tasks ) {
        terminatedCount += task.wasTerminated() ? 1 : 0;
      }
      this.terminatedCount = terminatedCount;

      try {
        autoScalingGroups.update(
            getOwner(),
            getGroup().getAutoScalingGroupName(),
            new Callback<AutoScalingGroup>(){
              @Override
              public void fire( final AutoScalingGroup autoScalingGroup ) {
                autoScalingGroup.setCapacity(
                    Math.max( 0, getCurrentCapacity() - TerminateInstancesScalingProcessTaskSupport.this.terminatedCount ) );
              }
            } );
      } catch ( AutoScalingMetadataNotFoundException e ) {
        // Not an error as user termination can be run when group is deleted
      } catch ( AutoScalingMetadataException e ) {
        logger.error( e, e );
      }
    }
  }

  private class TerminateInstancesScalingProcessTask extends TerminateInstancesScalingProcessTaskSupport {
    private final int currentCapacity;
    private final boolean replace;

    TerminateInstancesScalingProcessTask( final AutoScalingGroup group,
                                          final int currentCapacity,
                                          final List<String> instanceIds,
                                          final List<ActivityCause> causes,
                                          final boolean replace,
                                          final boolean persist,
                                          final boolean scaling ) {
      super( group, "Terminate", instanceIds, causes, persist, scaling );
      this.currentCapacity = currentCapacity;
      this.replace = replace;
    }

    @Override
    ScalingProcessTask onSuccess() {
      return replace ?
          new LaunchInstancesScalingProcessTask(
              getGroup(),
              getTerminatedCount(),
              String.format( "an instance was started in response to a difference between desired and actual capacity, increasing the capacity from %1$d to %2$d",
                  getGroup().getCapacity( ) - getTerminatedCount( ), // The group here has the original capacity value
                  getGroup().getCapacity( ) ) ) :
          null;
    }

    @Override
    int getCurrentCapacity() {
      return currentCapacity;
    }
  }

  private class UserTerminateInstancesScalingProcessTask extends TerminateInstancesScalingProcessTaskSupport {

    UserTerminateInstancesScalingProcessTask( final AutoScalingGroup group,
                                              final List<String> instanceIds ) {
      super( group, "UserTermination", instanceIds, Collections.singletonList( new ActivityCause("instance was taken out of service in response to a user request.") ), true, false );
    }
  }

  private class UserRemoveFromLoadBalancerScalingProcessTask extends RemoveFromLoadBalancerScalingProcessTask {

    UserRemoveFromLoadBalancerScalingProcessTask( final AutoScalingGroup group,
                                                  final List<String> instanceIds ) {
      super( UUID.randomUUID().toString(), group, "UserRemoveFromLoadBalancer", instanceIds, Collections.<ActivityCause>emptyList() );
    }

    @Override
    ScalingProcessTask onSuccess() {
      return null;
    }
  }

  private class UntrackedInstanceTerminationScalingActivityTask extends ScalingActivityTask<DescribeTagsResponseType> {
    private final AtomicReference<Multimap<String,String>> knownAutoScalingInstanceIds = new AtomicReference<Multimap<String,String>>(
        HashMultimap.<String,String>create()
    );

    UntrackedInstanceTerminationScalingActivityTask( final ScalingActivity activity ) {
      super( activity, false );
    }

    @Override
    void dispatchInternal( final ActivityContext context, final Callback.Checked<DescribeTagsResponseType> callback ) {
      final EucalyptusClient client = context.getEucalyptusClient();
      client.dispatch( describeTags( ), callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context, final DescribeTagsResponseType response ) {
      final Multimap<String,String> instanceMap = HashMultimap.create();
      if ( response.getTagSet() != null ) for ( final TagInfo tagInfo : response.getTagSet() ) {
        if ( "aws:autoscaling:groupName".equals( tagInfo.getKey() ) &&
            "instance".equals( tagInfo.getResourceType() ) ) {
          final String instanceId = tagInfo.getResourceId();
          final String groupName = tagInfo.getValue();
          instanceMap.put( groupName, instanceId );
        }
      }
      knownAutoScalingInstanceIds.set( Multimaps.unmodifiableMultimap( instanceMap ) );
      setActivityFinalStatus( ActivityStatusCode.Successful );
    }
  }

  private class UntrackedInstanceTerminationScalingProcessTask extends ScalingProcessTask<UntrackedInstanceTerminationScalingActivityTask> {
    private volatile String groupName;
    private volatile List<String> instanceIds;

    UntrackedInstanceTerminationScalingProcessTask( final AutoScalingGroup group ) {
      super( group.getOwnerAccountNumber(), group, "UntrackedInstanceTermination" );
    }

    @Override
    ScalingProcessTask onSuccess() {
      TerminateInstancesScalingProcessTask terminateTask = null;
      if ( groupName != null ) {
        AutoScalingGroup group = null;
        if ( groupName.equals( getGroup().getAutoScalingGroupName() ) ) {
          group = getGroup();
        } else try {
          group = autoScalingGroups.lookup( getGroup().getOwner(), groupName );
        } catch ( AutoScalingMetadataNotFoundException e ) {
          // Expected if the group was deleted
          group = AutoScalingGroup.named( getGroup().getOwner(), groupName );
          group.setCapacity( 0 );
        } catch ( Exception e ) {
          logger.error( e, e );
        }
        if ( group != null ) {
          logger.info( "Terminating untracked auto scaling instances: " + instanceIds );
          terminateTask = new TerminateInstancesScalingProcessTask( group, group.getCapacity(), instanceIds, Collections.<ActivityCause>emptyList(), false, false, false ){
            @Override
            void partialSuccess( final List<TerminateInstanceScalingActivityTask> tasks ) {
              // no update required, we were not tracking the instance(s)
            }
          };
        }
      }
      return terminateTask;
    }

    @Override
    boolean shouldRun() {
      return true;
    }

    @Override
    List<UntrackedInstanceTerminationScalingActivityTask> buildActivityTasks() throws AutoScalingMetadataException {
      return Collections.singletonList( new UntrackedInstanceTerminationScalingActivityTask( newActivity() ) );
    }

    @Override
    void partialSuccess( final List<UntrackedInstanceTerminationScalingActivityTask> tasks ) {
      final Multimap<String,String> groupNameToInstances = HashMultimap.create();
      final Set<String> taggedInstanceIds = Sets.newHashSet();
      for ( final UntrackedInstanceTerminationScalingActivityTask task : tasks ) {
        groupNameToInstances.putAll( task.knownAutoScalingInstanceIds.get() );
        taggedInstanceIds.addAll( task.knownAutoScalingInstanceIds.get().values() );
      }

      try {
        final Set<String> knownInstanceIds =
            autoScalingInstances.verifyInstanceIds( getGroup().getOwnerAccountNumber(), taggedInstanceIds );
        groupNameToInstances.values().removeAll( knownInstanceIds );

        final Map<String,Collection<String>> groupMap = groupNameToInstances.asMap();
        int entryIndex = -1;
        if ( groupMap.size() == 1 ) {
          entryIndex = 0;
        } else if ( !groupMap.isEmpty() ) {
          final Random random = new Random();
          entryIndex = random.nextInt( groupMap.size() );
        }

        if ( entryIndex >= 0 ) {
          final Map.Entry<String,Collection<String>> entry =
              Iterables.get( groupMap.entrySet(), entryIndex );
          this.groupName = entry.getKey();
          this.instanceIds = Lists.newArrayList( entry.getValue() );
        }
      } catch ( Exception e ) {
        logger.error( e, e );
      }
    }
  }

  private class MonitoringScalingActivityTask extends ScalingActivityTask<DescribeInstanceStatusResponseType> {
    private final List<String> instanceIds;
    private final AtomicReference<List<String>> healthyInstanceIds = new AtomicReference<List<String>>(
        Collections.<String>emptyList()
    );

    private MonitoringScalingActivityTask( final ScalingActivity activity,
                                           final List<String> instanceIds ) {
      super( activity, false );
      this.instanceIds = instanceIds;
    }

    @Override
    void dispatchInternal( final ActivityContext context,
                           final Callback.Checked<DescribeInstanceStatusResponseType> callback ) {
      final EucalyptusClient client = context.getEucalyptusClient();
      client.dispatch( monitorInstances( instanceIds ), callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final DescribeInstanceStatusResponseType response ) {
      final List<String> healthyInstanceIds = Lists.newArrayList();
      if ( response.getInstanceStatusSet() != null &&
          response.getInstanceStatusSet().getItem() != null ) {
        for ( final InstanceStatusItemType instanceStatus : response.getInstanceStatusSet().getItem() ){
          if ( instanceStatus.getInstanceStatus() != null &&
              "ok".equals( instanceStatus.getInstanceStatus().getStatus() ) ) {
            healthyInstanceIds.add( instanceStatus.getInstanceId() );
          }
        }
      }

      this.healthyInstanceIds.set( ImmutableList.copyOf( healthyInstanceIds ) );

      setActivityFinalStatus( ActivityStatusCode.Successful );
    }

    List<String> getHealthyInstanceIds() {
      return healthyInstanceIds.get();
    }
  }

  private class MonitoringScalingProcessTask extends ScalingProcessTask<MonitoringScalingActivityTask> {
    private final List<String> pendingInstanceIds;
    private final List<String> expectedRunningInstanceIds;

    MonitoringScalingProcessTask( final AutoScalingGroup group,
                                  final List<String> pendingInstanceIds,
                                  final List<String> expectedRunningInstanceIds ) {
      super( group, "Monitor" );
      this.pendingInstanceIds = pendingInstanceIds;
      this.expectedRunningInstanceIds = scalingProcessEnabled( ScalingProcessType.HealthCheck, group ) ?
          expectedRunningInstanceIds :
          Collections.<String>emptyList();
    }

    @Override
    boolean shouldRun() {
      return !expectedRunningInstanceIds.isEmpty() || !pendingInstanceIds.isEmpty();
    }

    @Override
    ScalingProcessTask onSuccess() {
      return getGroup().getLoadBalancerNames().isEmpty() || HealthCheckType.ELB != getGroup().getHealthCheckType() ?
          null :
          new ElbMonitoringScalingProcessTask(
              getGroup(),
              getGroup().getLoadBalancerNames(),
              expectedRunningInstanceIds );
    }

    @Override
    List<MonitoringScalingActivityTask> buildActivityTasks() throws AutoScalingMetadataException {
      final List<String> instanceIds = Lists.newArrayList( Iterables.concat(
          pendingInstanceIds,
          expectedRunningInstanceIds
      ) );
      return Collections.singletonList( new MonitoringScalingActivityTask( newActivity(), instanceIds ) );
    }

    @Override
    void partialSuccess( final List<MonitoringScalingActivityTask> tasks ) {
      final Set<String> transitionToHealthy = Sets.newHashSet( pendingInstanceIds );
      final Set<String> healthyInstanceIds = Sets.newHashSet();

      for ( final MonitoringScalingActivityTask task : tasks ) {
        healthyInstanceIds.addAll( task.getHealthyInstanceIds() );
      }

      transitionToHealthy.retainAll( healthyInstanceIds );

      if ( scalingProcessEnabled( ScalingProcessType.HealthCheck, getGroup() ) ) try {
        autoScalingInstances.markMissingInstancesUnhealthy( getGroup(), healthyInstanceIds );
      } catch ( AutoScalingMetadataException e ) {
        logger.error( e, e );
      }

      if ( !transitionToHealthy.isEmpty() ) try {
        autoScalingInstances.transitionState(
            getGroup(),
            LifecycleState.Pending,
            LifecycleState.InService,
            transitionToHealthy );
      } catch ( AutoScalingMetadataException e ) {
        logger.error( e, e );
      }
    }
  }

  private class MetricsSubmissionScalingActivityTask extends ScalingActivityTask<PutMetricDataResponseType> {
    private final List<AutoScalingInstance> autoScalingInstances;

    private MetricsSubmissionScalingActivityTask( final ScalingActivity activity,
                                                  final List<AutoScalingInstance> autoScalingInstances ) {
      super( activity, false );
      this.autoScalingInstances = autoScalingInstances;
    }

    @Override
    void dispatchInternal( final ActivityContext context,
                           final Callback.Checked<PutMetricDataResponseType> callback ) {
      final CloudWatchClient client = context.getCloudWatchClient();
      final Date date = new Date();
      final MetricData metricData = new MetricData();
      for ( final MetricCollectionType metricCollectionType : getGroup().getEnabledMetrics() ) {
        final MetricDatum metricDatum = new MetricDatum();
        metricDatum.setDimensions( new Dimensions(
            new Dimension( "AutoScalingGroupName", getGroup().getAutoScalingGroupName() )
        ) );
        metricDatum.setTimestamp( date );
        metricDatum.setUnit( "None" );
        metricDatum.setMetricName( metricCollectionType.getDisplayName() );
        metricDatum.setValue( metricCollectionType.getValue( getGroup(), autoScalingInstances ) );
        metricData.getMember().add( metricDatum );
      }
      final PutMetricDataType putMetricData = new PutMetricDataType();
      putMetricData.setNamespace( "AWS/AutoScaling" );
      putMetricData.setMetricData( metricData );
      client.dispatch( putMetricData, callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final PutMetricDataResponseType response ) {
      setActivityFinalStatus( ActivityStatusCode.Successful );
    }
  }

  private class MetricsSubmissionScalingProcessTask extends ScalingProcessTask<MetricsSubmissionScalingActivityTask> {
    private final List<AutoScalingInstance> autoScalingInstances;

    MetricsSubmissionScalingProcessTask( final AutoScalingGroup group,
                                         final List<AutoScalingInstance> autoScalingInstances ) {
      super( group, "MetricsSubmission" );
      this.autoScalingInstances = autoScalingInstances;
    }

    @Override
    boolean shouldRun() {
      return !getGroup().getEnabledMetrics().isEmpty();
    }

    @Override
    List<MetricsSubmissionScalingActivityTask> buildActivityTasks() throws AutoScalingMetadataException {
      return Collections.singletonList( new MetricsSubmissionScalingActivityTask( newActivity(), autoScalingInstances ) );
    }
  }

  private class ElbMonitoringScalingActivityTask extends ScalingActivityTask<DescribeInstanceHealthResponseType> {
    private final String loadBalancerName;
    private final AtomicReference<List<String>> unhealthyInstanceIds = new AtomicReference<List<String>>(
        Collections.<String>emptyList()
    );

    private ElbMonitoringScalingActivityTask( final ScalingActivity activity,
                                              final String loadBalancerName ) {
      super( activity, false );
      this.loadBalancerName = loadBalancerName;
    }

    @Override
    void dispatchInternal( final ActivityContext context,
                           final Callback.Checked<DescribeInstanceHealthResponseType> callback ) {
      final ElbClient client = context.getElbClient();
      client.dispatch( describeInstanceHealth( loadBalancerName ), callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final DescribeInstanceHealthResponseType response ) {
      final List<String> unhealthyInstanceIds = Lists.newArrayList();
      if ( response.getDescribeInstanceHealthResult() != null &&
          response.getDescribeInstanceHealthResult().getInstanceStates() != null &&
          response.getDescribeInstanceHealthResult().getInstanceStates().getMember() != null) {
        for ( final com.eucalyptus.loadbalancing.InstanceState instanceStatus : response.getDescribeInstanceHealthResult().getInstanceStates().getMember() ){
          if ( "OutOfService".equals( instanceStatus.getState() ) ) {
            unhealthyInstanceIds.add( instanceStatus.getInstanceId() );
          }
        }
      }

      this.unhealthyInstanceIds.set( ImmutableList.copyOf( unhealthyInstanceIds ) );

      setActivityFinalStatus( ActivityStatusCode.Successful );
    }

    List<String> getUnhealthyInstanceIds() {
      return unhealthyInstanceIds.get();
    }
  }

  private class ElbMonitoringScalingProcessTask extends ScalingProcessTask<ElbMonitoringScalingActivityTask> {
    private final List<String> loadBalancerNames;
    private final List<String> expectedInstanceIds;

    ElbMonitoringScalingProcessTask( final AutoScalingGroup group,
                                     final List<String> loadBalancerNames,
                                     final List<String> expectedInstanceIds ) {
      super( group, "ElbMonitor" );
      this.loadBalancerNames = loadBalancerNames;
      this.expectedInstanceIds = expectedInstanceIds;
    }

    @Override
    boolean shouldRun() {
      return !loadBalancerNames.isEmpty() && !expectedInstanceIds.isEmpty();
    }

    @Override
    List<ElbMonitoringScalingActivityTask> buildActivityTasks() throws AutoScalingMetadataException {
      final List<ElbMonitoringScalingActivityTask> activities = Lists.newArrayList();
      for ( final String loadBalancerName : loadBalancerNames ) {
        activities.add( new ElbMonitoringScalingActivityTask( newActivity(), loadBalancerName ) );
      }
      return activities;
    }

    @Override
    void partialSuccess( final List<ElbMonitoringScalingActivityTask> tasks ) {
      final List<String> healthyInstanceIds = Lists.newArrayList( expectedInstanceIds );

      for ( final ElbMonitoringScalingActivityTask task : tasks ) {
        healthyInstanceIds.removeAll( task.getUnhealthyInstanceIds() );
      }

      try {
        autoScalingInstances.markMissingInstancesUnhealthy( getGroup(), healthyInstanceIds );
      } catch ( AutoScalingMetadataException e ) {
        logger.error( e, e );
      }
    }
  }

  private abstract class ValidationScalingActivityTask<RES extends BaseMessage> extends ScalingActivityTask<RES> {
    private final String description;
    private final AtomicReference<List<String>> validationErrors = new AtomicReference<List<String>>(
        Collections.<String>emptyList()
    );

    private ValidationScalingActivityTask( final ScalingActivity activity,
                                           final String description ) {
      super( activity, false );
      this.description = description;
    }

    @Override
    void dispatchFailure( final ActivityContext context, final Throwable throwable ) {
      super.dispatchFailure( context, throwable );
      handleValidationFailure( throwable );
    }

    void handleValidationFailure( final Throwable throwable ) {
      setValidationError( "Error validating " + description );
    }

    void setValidationError( final String error ) {
      validationErrors.set( ImmutableList.of( error ) );
    }

    List<String> getValidationErrors() {
      return validationErrors.get();
    }
  }

  private class AZValidationScalingActivityTask extends ValidationScalingActivityTask<DescribeAvailabilityZonesResponseType> {
    final List<String> availabilityZones;

    private AZValidationScalingActivityTask( final ScalingActivity activity,
                                             final List<String> availabilityZones ) {
      super( activity, "availability zone(s)" );
      this.availabilityZones = availabilityZones;
    }

    @Override
    void dispatchInternal( final ActivityContext context,
                           final Callback.Checked<DescribeAvailabilityZonesResponseType> callback ) {
      final EucalyptusClient client = context.getEucalyptusClient();

      final DescribeAvailabilityZonesType describeAvailabilityZonesType
          = new DescribeAvailabilityZonesType();
      describeAvailabilityZonesType.setAvailabilityZoneSet( Lists.newArrayList( availabilityZones ) );

      client.dispatch( describeAvailabilityZonesType, callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final DescribeAvailabilityZonesResponseType response ) {
      if ( response.getAvailabilityZoneInfo() == null ) {
        setValidationError( "Invalid availability zone(s): " + availabilityZones );
      } else if ( response.getAvailabilityZoneInfo().size() != availabilityZones.size() ) {
        final Set<String> zones = Sets.newHashSet();
        for ( final ClusterInfoType clusterInfoType : response.getAvailabilityZoneInfo() ) {
          zones.add( clusterInfoType.getZoneName() );
        }
        final Set<String> invalidZones = Sets.newTreeSet( availabilityZones );
        invalidZones.removeAll( zones );
        setValidationError( "Invalid availability zone(s): " + invalidZones );
      }

      setActivityFinalStatus( ActivityStatusCode.Successful );
    }
  }

  private class LoadBalancerValidationScalingActivityTask extends ValidationScalingActivityTask<DescribeLoadBalancersResponseType> {
    final List<String> loadBalancerNames;

    private LoadBalancerValidationScalingActivityTask( final ScalingActivity activity,
                                                       final List<String> loadBalancerNames ) {
      super( activity, "load balancer name(s)" );
      this.loadBalancerNames = loadBalancerNames;
    }

    @Override
    void dispatchInternal( final ActivityContext context,
                           final Callback.Checked<DescribeLoadBalancersResponseType> callback ) {
      final ElbClient client = context.getElbClient();

      final LoadBalancerNames loadBalancerNamesType = new LoadBalancerNames();
      loadBalancerNamesType.setMember( Lists.newArrayList( loadBalancerNames ) );
      final DescribeLoadBalancersType describeLoadBalancersType
          = new DescribeLoadBalancersType();
      describeLoadBalancersType.setLoadBalancerNames( loadBalancerNamesType );

      client.dispatch( describeLoadBalancersType, callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final DescribeLoadBalancersResponseType response ) {
      if ( response.getDescribeLoadBalancersResult() == null ||
          response.getDescribeLoadBalancersResult().getLoadBalancerDescriptions() == null ) {
        setValidationError( "Invalid load balancer name(s): " + loadBalancerNames );
      } else if ( response.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember().size() != loadBalancerNames.size() ) {
        final Set<String> loadBalancers = Sets.newHashSet();
        for ( final LoadBalancerDescription loadBalancerDescription :
            response.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember() ) {
          loadBalancers.add( loadBalancerDescription.getLoadBalancerName() );
        }
        final Set<String> invalidLoadBalancers = Sets.newTreeSet( loadBalancerNames );
        invalidLoadBalancers.removeAll( loadBalancers );
        setValidationError( "Invalid load balancer name(s): " + invalidLoadBalancers );
      }

      setActivityFinalStatus( ActivityStatusCode.Successful );
    }

    @Override
    void handleValidationFailure( final Throwable throwable ) {
      //TODO: Handle AccessPointNotFound if/when ELB service implements it
      super.handleValidationFailure( throwable );
    }
  }

  private class ImageIdValidationScalingActivityTask extends ValidationScalingActivityTask<DescribeImagesResponseType> {
    final List<String> imageIds;

    private ImageIdValidationScalingActivityTask( final ScalingActivity activity,
                                                  final List<String> imageIds ) {
      super( activity, "image id(s)" );
      this.imageIds = imageIds;
    }

    @Override
    void dispatchInternal( final ActivityContext context,
                           final Callback.Checked<DescribeImagesResponseType> callback ) {
      final EucalyptusClient client = context.getEucalyptusClient();

      final DescribeImagesType describeImagesType
          = new DescribeImagesType();
      describeImagesType.getFilterSet().add( filter( "image-id", imageIds ) );

      client.dispatch( describeImagesType, callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final DescribeImagesResponseType response ) {
      if ( response.getImagesSet() == null ) {
        setValidationError( "Invalid image id(s): " + imageIds );
      } else if ( response.getImagesSet().size() != imageIds.size() ) {
        final Set<String> images = Sets.newHashSet();
        for ( final ImageDetails imageDetails : response.getImagesSet() ) {
          images.add( imageDetails.getImageId() );
        }
        final Set<String> invalidImages = Sets.newTreeSet( imageIds );
        invalidImages.removeAll( images );
        setValidationError( "Invalid image id(s): " + invalidImages );
      }

      setActivityFinalStatus( ActivityStatusCode.Successful );
    }
  }

  private class InstanceTypeValidationScalingActivityTask extends ValidationScalingActivityTask<DescribeVmTypesResponseType> {
    final String instanceType;

    private InstanceTypeValidationScalingActivityTask( final ScalingActivity activity,
                                                       final String instanceType ) {
      super( activity, "instance type" );
      this.instanceType = instanceType;
    }

    @Override
    void dispatchInternal( final ActivityContext context,
                           final Callback.Checked<DescribeVmTypesResponseType> callback ) {
      final VmTypesClient client = context.getVmTypesClient( );
      client.dispatch( new DescribeVmTypesType( Collections.singleton( instanceType ) ), callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final DescribeVmTypesResponseType response ) {
      if ( response.getVmTypeDetails() == null || response.getVmTypeDetails().size() != 1 ) {
        setValidationError( "Invalid instance type: " + instanceType );
      }

      setActivityFinalStatus( ActivityStatusCode.Successful );
    }
  }

  private class SshKeyValidationScalingActivityTask extends ValidationScalingActivityTask<DescribeKeyPairsResponseType> {
    final String sshKey;

    private SshKeyValidationScalingActivityTask( final ScalingActivity activity,
                                                 final String sshKey ) {
      super( activity, "ssh key" );
      this.sshKey = sshKey;
    }

    @Override
    void dispatchInternal( final ActivityContext context,
                           final Callback.Checked<DescribeKeyPairsResponseType> callback ) {
      final EucalyptusClient client = context.getEucalyptusClient( );

      final DescribeKeyPairsType describeKeyPairsType
          = new DescribeKeyPairsType();
      describeKeyPairsType.getFilterSet().add( filter( "key-name", sshKey ) );

      client.dispatch( describeKeyPairsType, callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final DescribeKeyPairsResponseType response ) {
      if ( response.getKeySet( ) == null || response.getKeySet( ).size() != 1 ) {
        setValidationError( "Invalid ssh key: " + sshKey );
      }

      setActivityFinalStatus( ActivityStatusCode.Successful );
    }
  }

  private class SecurityGroupValidationScalingActivityTask extends ValidationScalingActivityTask<DescribeSecurityGroupsResponseType> {
    final List<String> groupNames;

    private SecurityGroupValidationScalingActivityTask( final ScalingActivity activity,
                                                        final List<String> groupNames ) {
      super( activity, "security group(s)" );
      this.groupNames = groupNames;
    }

    @Override
    void dispatchInternal( final ActivityContext context,
                           final Callback.Checked<DescribeSecurityGroupsResponseType> callback ) {
      final EucalyptusClient client = context.getEucalyptusClient();

      final DescribeSecurityGroupsType describeSecurityGroupsType
          = new DescribeSecurityGroupsType();
      describeSecurityGroupsType.getFilterSet().add( filter( "group-name", groupNames ) );

      client.dispatch( describeSecurityGroupsType, callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final DescribeSecurityGroupsResponseType response ) {
      if ( response.getSecurityGroupInfo() == null ) {
        setValidationError( "Invalid security group(s): " + groupNames );
      } else if ( response.getSecurityGroupInfo().size() != groupNames.size() ) {
        final Set<String> groups = Sets.newHashSet();
        for ( final SecurityGroupItemType securityGroupItemType : response.getSecurityGroupInfo() ) {
          groups.add( securityGroupItemType.getGroupName() );
        }
        final Set<String> invalidGroups = Sets.newTreeSet( groupNames );
        invalidGroups.removeAll( groups );
        setValidationError( "Invalid security group(s): " + invalidGroups );
      }

      setActivityFinalStatus( ActivityStatusCode.Successful );
    }
  }

  private class ValidationScalingProcessTask extends ScalingProcessTask<ValidationScalingActivityTask<?>> {
    private final List<String> availabilityZones;
    private final List<String> loadBalancerNames;
    private final List<String> imageIds;
    private final List<String> securityGroups;
    @Nullable
    private final String instanceType;
    @Nullable
    private final String keyName;
    private final AtomicReference<List<String>> validationErrors = new AtomicReference<List<String>>(
        Collections.<String>emptyList()
    );

    ValidationScalingProcessTask( final OwnerFullName owner,
                                  final List<String> availabilityZones,
                                  final List<String> loadBalancerNames,
                                  final List<String> imageIds,
                                  @Nullable final String instanceType,
                                  @Nullable final String keyName,
                                  final List<String> securityGroups ) {
      super( UUID.randomUUID().toString() + "-validation", AutoScalingGroup.withOwner(owner), "Validate" );
      this.availabilityZones = availabilityZones;
      this.loadBalancerNames = loadBalancerNames;
      this.imageIds = imageIds;
      this.instanceType = instanceType;
      this.keyName = keyName;
      this.securityGroups = securityGroups;
    }

    @Override
    boolean shouldRun() {
      return
          !availabilityZones.isEmpty() ||
          !loadBalancerNames.isEmpty() ||
          !imageIds.isEmpty() ||
          instanceType != null ||
          keyName != null ||
          !securityGroups.isEmpty();
    }

    @Override
    List<ValidationScalingActivityTask<?>> buildActivityTasks() throws AutoScalingMetadataException {
      final List<ValidationScalingActivityTask<?>> tasks = Lists.newArrayList();
      if ( !availabilityZones.isEmpty() ) {
        tasks.add( new AZValidationScalingActivityTask( newActivity(), availabilityZones ) );
      }
      if ( !loadBalancerNames.isEmpty() ) {
        tasks.add( new LoadBalancerValidationScalingActivityTask( newActivity(), loadBalancerNames ) );
      }
      if ( !imageIds.isEmpty() ) {
        tasks.add( new ImageIdValidationScalingActivityTask( newActivity(), imageIds ) );
      }
      if ( instanceType != null ) {
        tasks.add( new InstanceTypeValidationScalingActivityTask( newActivity(), instanceType ) );
      }
      if ( keyName != null ) {
        tasks.add( new SshKeyValidationScalingActivityTask( newActivity(), keyName ) );
      }
      if ( !securityGroups.isEmpty() ) {
        tasks.add( new SecurityGroupValidationScalingActivityTask( newActivity(), securityGroups ) );
      }
      return tasks;
    }

    @Override
    void partialSuccess( final List<ValidationScalingActivityTask<?>> tasks ) {
      final List<String> validationErrors = Lists.newArrayList( );
      for ( final ValidationScalingActivityTask<?> task : tasks ) {
        validationErrors.addAll( task.getValidationErrors() );
      }
      this.validationErrors.set( ImmutableList.copyOf( validationErrors ) );
    }

    List<String> getValidationErrors() {
      return validationErrors.get();
    }
  }

  private class AlarmLookupActivityTask extends ScalingActivityTask<DescribeAlarmsResponseType> {
    private final String policyArn;
    private final AtomicReference<Collection<String>> alarmArns = new AtomicReference<Collection<String>>(
        Collections.<String>emptyList()
    );

    private AlarmLookupActivityTask( final ScalingActivity activity,
                                     final String policyArn ) {
      super( activity, false );
      this.policyArn = policyArn;
    }

    @Override
    void dispatchInternal( final ActivityContext context,
                           final Callback.Checked<DescribeAlarmsResponseType> callback ) {
      final CloudWatchClient client = context.getCloudWatchClient();
      final DescribeAlarmsType describeAlarmsType = new DescribeAlarmsType();
      describeAlarmsType.setActionPrefix( policyArn );
      client.dispatch( describeAlarmsType, callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final DescribeAlarmsResponseType response ) {
      final List<String> arns = Lists.newArrayList();
      if ( response.getDescribeAlarmsResult() != null && response.getDescribeAlarmsResult().getMetricAlarms() != null ) {
        for ( final MetricAlarm metricAlarm : response.getDescribeAlarmsResult().getMetricAlarms().getMember() ) {
          final ResourceList list = metricAlarm.getAlarmActions();
          if ( list != null && list.getMember().contains( policyArn ) ) {
            arns.add( metricAlarm.getAlarmArn() );
          }
        }
      }

      alarmArns.set( arns );

      setActivityFinalStatus( ActivityStatusCode.Successful );
    }

    String getPolicyArn() {
      return policyArn;
    }

    Collection<String> getAlarmArns() {
      return alarmArns.get();
    }
  }

  private class AlarmLookupProcessTask extends ScalingProcessTask<AlarmLookupActivityTask> {
    private final List<String> policyArns;
    private final AtomicReference<Map<String,Collection<String>>> policyArnToAlarmArns = new AtomicReference<Map<String,Collection<String>>>(
        Collections.<String,Collection<String>>emptyMap( )
    );

    AlarmLookupProcessTask( final OwnerFullName owner,
                            final List<String> policyArns ) {
      super( UUID.randomUUID().toString() + "-alarm-lookup", AutoScalingGroup.withOwner(owner), "AlarmLookup" );
      this.policyArns = policyArns;
    }

    @Override
    boolean shouldRun() {
      return !policyArns.isEmpty();
    }

    @Override
    List<AlarmLookupActivityTask> buildActivityTasks() throws AutoScalingMetadataException {
      final List<AlarmLookupActivityTask> tasks = Lists.newArrayList();
      for ( final String policyArn : policyArns ) {
        tasks.add( new AlarmLookupActivityTask( newActivity(), policyArn ) );
      }
      return tasks;
    }

    @Override
    void partialSuccess( final List<AlarmLookupActivityTask> tasks ) {
      final Map<String,Collection<String>> policyArnToAlarmArns = Maps.newHashMap();
      for ( final AlarmLookupActivityTask task : tasks ) {
        policyArnToAlarmArns.put( task.getPolicyArn(), task.getAlarmArns() );
      }
      this.policyArnToAlarmArns.set( ImmutableMap.copyOf( policyArnToAlarmArns ) );
    }

    Map<String,Collection<String>> getPolicyArnToAlarmArns() {
      return policyArnToAlarmArns.get();
    }
  }

  private static abstract class ScalingTask {
    private volatile int count = 0;
    private final int factor;
    private final ActivityTask task;

    ScalingTask( int factor, ActivityTask task ) {
      this.factor = factor;
      this.task = task;
    }

    int calcFactor() {
      return factor / (int) Math.max( 1, SystemClock.RATE / 1000 );
    }

    void perhapsWork() throws Exception {
      if ( ++count % calcFactor() == 0 && !AutoScalingConfiguration.getSuspendedTasks().contains( task ) ) {
        doWork();
      }
    }

    abstract void doWork( ) throws Exception;
  }

  private enum CauseTransform implements Function<GroupScalingCause,ActivityCause> {
    INSTANCE;

    @Override
    public ActivityCause apply( final GroupScalingCause groupScalingCause ) {
      return new ActivityCause( groupScalingCause.getTimestamp(), groupScalingCause.getDetail() );
    }
  }

  public static class ActivityManagerEventListener implements EventListener<ClockTick> {
    private final ActivityManager activityManager = new ActivityManager();

    public static void register( ) {
      Listeners.register( ClockTick.class, new ActivityManagerEventListener() );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      if ( Bootstrap.isFinished() &&
          Topology.isEnabledLocally( AutoScaling.class ) &&
          Topology.isEnabled( Eucalyptus.class ) ) {
        activityManager.doScaling();
      }
    }
  }
}
