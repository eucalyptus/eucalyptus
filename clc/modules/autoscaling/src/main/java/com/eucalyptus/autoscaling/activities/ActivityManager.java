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
import static com.eucalyptus.autoscaling.instances.AutoScalingInstances.availabilityZone;
import static com.eucalyptus.autoscaling.instances.AutoScalingInstances.instanceId;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.autoscaling.groups.AutoScalingGroups;
import com.eucalyptus.autoscaling.groups.PersistenceAutoScalingGroups;
import com.eucalyptus.autoscaling.groups.TerminationPolicyType;
import com.eucalyptus.autoscaling.instances.AutoScalingInstance;
import com.eucalyptus.autoscaling.instances.AutoScalingInstances;
import com.eucalyptus.autoscaling.instances.LifecycleState;
import com.eucalyptus.autoscaling.instances.PersistenceAutoScalingInstances;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataNotFoundException;
import com.eucalyptus.autoscaling.tags.Tag;
import com.eucalyptus.autoscaling.tags.TagSupport;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.loadbalancing.DescribeLoadBalancersResponseType;
import com.eucalyptus.loadbalancing.DescribeLoadBalancersType;
import com.eucalyptus.loadbalancing.LoadBalancerDescription;
import com.eucalyptus.loadbalancing.LoadBalancerNames;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import edu.ucsb.eucalyptus.msgs.Filter;
import edu.ucsb.eucalyptus.msgs.ImageDetails;
import edu.ucsb.eucalyptus.msgs.InstanceStatusItemType;
import edu.ucsb.eucalyptus.msgs.ResourceTag;
import edu.ucsb.eucalyptus.msgs.RunInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;
import edu.ucsb.eucalyptus.msgs.SecurityGroupItemType;
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

  private static final long activityTimeout = TimeUnit.MINUTES.toMillis( 5 );

  private static final int maxLaunchIncrement = 20;

  private final ScalingActivities scalingActivities;
  private final AutoScalingGroups autoScalingGroups;
  private final AutoScalingInstances autoScalingInstances;
  private final BackoffRunner runner = BackoffRunner.getInstance( );

  public ActivityManager() {
    this(
        new PersistenceScalingActivities( ),
        new PersistenceAutoScalingGroups( ),
        new PersistenceAutoScalingInstances( ) );
  }

  protected ActivityManager( final ScalingActivities scalingActivities,
                             final AutoScalingGroups autoScalingGroups,
                             final AutoScalingInstances autoScalingInstances ) {
    this.scalingActivities = scalingActivities;
    this.autoScalingGroups = autoScalingGroups;
    this.autoScalingInstances = autoScalingInstances;
  }

  public void doScaling() {
    timeoutScalingActivities( );

    // Restore old termination attempts
    try {
      final List<AutoScalingInstance> failedToTerminate =
          autoScalingInstances.listByState( LifecycleState.Terminating );
      final Iterable<String> groupArns = Iterables.transform( failedToTerminate, AutoScalingInstances.groupArn() );
      for ( final String groupArn : groupArns ) {
        final Iterable<AutoScalingInstance> groupInstances =
            Iterables.filter( failedToTerminate, CollectionUtils.propertyPredicate( groupArn, AutoScalingInstances.groupArn() ) );
        runTask( terminateInstancesTask( groupInstances ) );
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }

    // Launch and terminate
    try {
      for ( final AutoScalingGroup group : autoScalingGroups.listRequiringScaling() ) {
        int compareResult = group.getCapacity().compareTo( group.getDesiredCapacity() );
        if ( compareResult < 0 ) {
          runTask( new LaunchInstancesScalingProcessTask( group ) );
        } else if ( compareResult > 0 ) {
          runTask( perhapsTerminateInstances( group, group.getCapacity() - group.getDesiredCapacity() ) );
        }
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }

    // Replace unhealthy
    try {
      for ( final AutoScalingGroup group : autoScalingGroups.listRequiringInstanceReplacement() ) {
        runTask( perhapsReplaceInstances( group ) ) ;
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }

    // Monitor instances
    try {
      for ( final AutoScalingGroup group : autoScalingGroups.listRequiringMonitoring( 10000L ) ) {
        final List<AutoScalingInstance> groupInstances = autoScalingInstances.listByGroup( group );
        runTask( new MonitoringScalingProcessTask(
            group,
            Lists.newArrayList( Iterables.transform( Iterables.filter( groupInstances, LifecycleState.Pending ), instanceId() ) ),
            Lists.newArrayList( Iterables.transform( Iterables.filter( groupInstances, LifecycleState.InService ), instanceId() ) )
        ) );
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }

    //TODO:STEVE: When do we delete old scaling activities? (retain 6 weeks of activities by default as per AWS)
    //TODO:STEVE: Do we need to find running instances with auto scaling tags that we are not tracking and terminate them?
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
    return task.getActivities();
  }

  public List<String> validateReferences( final OwnerFullName owner,
                                          final Collection<String> availabilityZones,
                                          final Collection<String> loadBalancerNames ) {
    return validateReferences(
        owner,
        Objects.firstNonNull( availabilityZones, Collections.<String>emptyList() ),
        Objects.firstNonNull( loadBalancerNames, Collections.<String>emptyList() ),
        Collections.<String>emptyList(),
        null ,
        Collections.<String>emptyList() );

  }

  public List<String> validateReferences( final OwnerFullName owner,
                                          final Iterable<String> imageIds,
                                          final String keyName,
                                          final Iterable<String> securityGroups ) {
    return validateReferences(
        owner,
        Collections.<String>emptyList(),
        Collections.<String>emptyList(),
        Objects.firstNonNull( imageIds, Collections.<String>emptyList() ),
        keyName,
        Objects.firstNonNull( securityGroups, Collections.<String>emptyList() ) );
  }

  private List<String> validateReferences( final OwnerFullName owner,
                                           final Iterable<String> availabilityZones,
                                           final Iterable<String> loadBalancerNames,
                                           final Iterable<String> imageIds,
                                           @Nullable final String keyName,
                                           final Iterable<String> securityGroups ) {
    final List<String> errors = Lists.newArrayList();

    final ValidationScalingProcessTask task = new ValidationScalingProcessTask(
        owner,
        Lists.newArrayList( Sets.newLinkedHashSet( availabilityZones ) ),
        Lists.newArrayList( Sets.newLinkedHashSet( loadBalancerNames ) ),
        Lists.newArrayList( Sets.newLinkedHashSet( imageIds ) ),
        keyName,
        Lists.newArrayList( Sets.newLinkedHashSet( securityGroups ) ) );
    runTask( task );
    try {
      final boolean success = task.getFuture().get();
      if ( success ) {
        errors.addAll( task.getValidationErrors() );
      } else {
        errors.add("Unable to validate references at this time.");
      }
    } catch ( ExecutionException e ) {
      logger.error( e, e );
      errors.add("Error during reference validation");
    } catch ( InterruptedException e ) {
      Thread.currentThread().interrupt();
      errors.add("Validation interrupted");
    }

    return errors;
  }

  private TerminateInstancesScalingProcessTask terminateInstancesTask( final Iterable<AutoScalingInstance> groupInstances ) {
    return new TerminateInstancesScalingProcessTask(
        Iterables.get( groupInstances, 0 ).getAutoScalingGroup(),
        Iterables.get( groupInstances, 0 ).getAutoScalingGroup().getCapacity(),
        Lists.newArrayList( Iterables.transform( groupInstances, AutoScalingInstances.instanceId() ) ),
        false );
  }

  private TerminateInstancesScalingProcessTask perhapsTerminateInstances( final AutoScalingGroup group,
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
        final Set<String> unwantedZones = Sets.newHashSet( Iterables.transform( currentInstances, availabilityZone() ) );
        unwantedZones.removeAll( group.getAvailabilityZones() );

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
            targetZones = Sets.newLinkedHashSet( group.getAvailabilityZones() );
          } else {
            targetZones = unwantedZones;
          }
        } else {
          targetZones = Sets.newLinkedHashSet( group.getAvailabilityZones() );
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
    return new TerminateInstancesScalingProcessTask( group, currentCapacity, instancesToTerminate, false );
  }

  private TerminateInstancesScalingProcessTask perhapsReplaceInstances( final AutoScalingGroup group ) {
    final List<String> instancesToTerminate = Lists.newArrayList();
    try {
      final List<AutoScalingInstance> currentInstances =
          autoScalingInstances.listUnhealthyByGroup( group );
      Iterables.addAll(
            instancesToTerminate,
            Iterables.limit(
              Iterables.transform( currentInstances, AutoScalingInstances.instanceId()),
                Math.min( maxLaunchIncrement, currentInstances.size() ) ) );
    } catch ( final Exception e ) {
      logger.error( e, e );
    }
    return new TerminateInstancesScalingProcessTask( group, group.getCapacity(), instancesToTerminate, true );
  }

  private RunInstancesType runInstances( final AutoScalingGroup group,
                                         final String availabilityZone,
                                         final int attemptToLaunch ) {
    final LaunchConfiguration launchConfiguration = group.getLaunchConfiguration();
    final RunInstancesType runInstances = TypeMappers.transform( launchConfiguration, RunInstancesType.class );
    runInstances.setAvailabilityZone( availabilityZone );
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

  /**
   * If scaling activities are not updated for some time we will fail them.
   *
   * Activities should not require this cleanup, this is an error case.
   */
  private void timeoutScalingActivities( ) {
    try {
      for ( final ScalingActivity activity : scalingActivities.list( null ) ) { //TODO:STEVE: don't list all activities
        if ( !completedActivityStates.contains( activity.getActivityStatusCode() ) &&
            isTimedOut( activity.getLastUpdateTimestamp() ) ) {
          scalingActivities.update( activity.getOwner(),
                                    activity.getActivityId(),
                                    new Callback<ScalingActivity>(){
                                      @Override
                                      public void fire( final ScalingActivity scalingActivity ) {
                                        scalingActivity.setActivityStatusCode( ActivityStatusCode.Cancelled );
                                        scalingActivity.setEndTime( new Date() );
                                      }
                                    } );
        }
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }
  }

  private boolean isTimedOut( final Date timestamp ) {
    return ( System.currentTimeMillis() - timestamp.getTime() ) > activityTimeout;
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

  private interface ActivityContext {
    String getUserId();
    EucalyptusClient getEucalyptusClient();
    ElbClient getElbClient();
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
      logger.error( "Activity error", throwable ); //TODO:STEVE: Remove failure logging and record in scaling activity details/description
      setActivityFinalStatus( ActivityStatusCode.Failed );
    }

    abstract void dispatchSuccess( ActivityContext context, RES response );

    void setActivityFinalStatus( final ActivityStatusCode activityStatusCode ) {
      final ScalingActivity activity = getActivity();
      if ( activity.getCreationTimestamp() != null ) { // only update if persistent
        try {
          scalingActivities.update(
              activity.getOwner(),
              activity.getActivityId(),
              new Callback<ScalingActivity>(){
                @Override
                public void fire( final ScalingActivity input ) {
                  input.setActivityStatusCode( activityStatusCode );
                  input.setEndTime( new Date() );
                }
              } );
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
    private final String activity;
    private final Supplier<String> userIdSupplier;
    private final AtomicReference<List<ScalingActivity>> activities =
        new AtomicReference<List<ScalingActivity>>( Collections.<ScalingActivity>emptyList() );
    private volatile CheckedListenableFuture<List<Boolean>> dispatchFuture;
    private volatile CheckedListenableFuture<Boolean> taskFuture;

    ScalingProcessTask( final String uniqueKey,
                        final AutoScalingGroup group,
                        final String activity ) {
      super( uniqueKey, activity );
      this.group = group;
      this.activity = activity;
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

    ScalingActivity newActivity() {
      return ScalingActivity.create( group, activity );
    }

    abstract boolean shouldRun();
    abstract List<AT> buildActivityTasks() throws AutoScalingMetadataException;

    @Override
    ScalingProcessTask onSuccess() {
      return null;
    }

    void partialSuccess( final List<AT> tasks ) {
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
          final CheckedListenableFuture<List<Boolean>> resultFuture = dispatchFuture = Futures.allAsList( dispatchFutures );
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
    private final AtomicReference<List<String>> instanceIds = new AtomicReference<List<String>>(
        Collections.<String>emptyList()
    );

    private LaunchInstanceScalingActivityTask( final ScalingActivity activity,
                                               final String availabilityZone ) {
      super( activity );
      this.availabilityZone = availabilityZone;
    }

    @Override
    void dispatchInternal( final ActivityContext context,
                           final Callback.Checked<RunInstancesResponseType> callback ) {
      final EucalyptusClient client = context.getEucalyptusClient();
      client.dispatch( runInstances( getGroup(), availabilityZone, 1 ), callback );
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

      setActivityFinalStatus( ActivityStatusCode.Successful );
    }

    List<String> getInstanceIds() {
      return instanceIds.get();
    }
  }

  private class LaunchInstancesScalingProcessTask extends ScalingProcessTask<LaunchInstanceScalingActivityTask> {
    private final int launchCount;

    LaunchInstancesScalingProcessTask( final AutoScalingGroup group ) {
      this( group, group.getDesiredCapacity() - group.getCapacity() );
    }

    LaunchInstancesScalingProcessTask( final AutoScalingGroup group,
                                       final int launchCount ) {
      super( group, "Launch" );
      this.launchCount = launchCount;
    }

    @Override
    boolean shouldRun() {
      return launchCount > 0;
    }

    @Override
    List<LaunchInstanceScalingActivityTask> buildActivityTasks() throws AutoScalingMetadataException {
      final List<AutoScalingInstance> instances = autoScalingInstances.listByGroup( getGroup() );
      final Map<String,Integer> zoneCounts =
          buildAvailabilityZoneInstanceCounts( instances, getGroup().getAvailabilityZones() );
      final int attemptToLaunch = Math.min( maxLaunchIncrement, launchCount );
      final List<LaunchInstanceScalingActivityTask> activities = Lists.newArrayList();
      for ( int i=0; i<attemptToLaunch; i++ ) {
        final Map.Entry<String,Integer> entry = selectEntry( zoneCounts, Ordering.natural() );
        entry.setValue( entry.getValue() + 1 );
        activities.add( new LaunchInstanceScalingActivityTask( newActivity(), entry.getKey() ) );
      }
      return activities;
    }

    @Override
    void partialSuccess( final List<LaunchInstanceScalingActivityTask> tasks ) {
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

  private class TerminateInstanceScalingActivityTask extends ScalingActivityTask<TerminateInstancesResponseType> {
    private final String instanceId;
    private volatile boolean terminated = false;

    private TerminateInstanceScalingActivityTask( final ScalingActivity activity,
                                                  final String instanceId ) {
      super( activity );
      this.instanceId = instanceId;
    }

    @Override
    void dispatchInternal( final ActivityContext context,
                           final Callback.Checked<TerminateInstancesResponseType> callback ) {
      final EucalyptusClient client = context.getEucalyptusClient();
      client.dispatch( terminateInstances( Collections.singleton(instanceId) ), callback );
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

  private abstract class TerminationInstancesScalingProcessTaskSupport extends ScalingProcessTask<TerminateInstanceScalingActivityTask> {
    private final List<String> instanceIds;
    private volatile int terminatedCount;

    TerminationInstancesScalingProcessTaskSupport( final AutoScalingGroup group,
                                                   final String activity,
                                                   final List<String> instanceIds ) {
      super( group, activity );
      this.instanceIds = instanceIds;
    }

    @Override
    boolean shouldRun() {
      return !instanceIds.isEmpty();
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
          activities.add( new TerminateInstanceScalingActivityTask( newActivity(), instanceId ) );
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
                    Math.max( 0, getCurrentCapacity() - TerminationInstancesScalingProcessTaskSupport.this.terminatedCount ) );
              }
            } );
      } catch ( AutoScalingMetadataNotFoundException e ) {
        // Not an error as user termination can be run when group is deleted
      } catch ( AutoScalingMetadataException e ) {
        logger.error( e, e );
      }
    }
  }

  private class TerminateInstancesScalingProcessTask extends TerminationInstancesScalingProcessTaskSupport {
    private final int currentCapacity;
    private final boolean replace;

    TerminateInstancesScalingProcessTask( final AutoScalingGroup group,
                                          final int currentCapacity,
                                          final List<String> instanceIds,
                                          final boolean replace ) {
      super( group, "Terminate", instanceIds );
      this.currentCapacity = currentCapacity;
      this.replace = replace;
    }

    @Override
    ScalingProcessTask onSuccess() {
      return replace ?
          new LaunchInstancesScalingProcessTask( getGroup(), getTerminatedCount() ) :
          null;
    }

    @Override
    int getCurrentCapacity() {
      return currentCapacity;
    }
  }

  private class UserTerminateInstancesScalingProcessTask extends TerminationInstancesScalingProcessTaskSupport {

    UserTerminateInstancesScalingProcessTask( final AutoScalingGroup group,
                                              final List<String> instanceIds ) {
      super( group, "UserTermination", instanceIds );
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
          healthyInstanceIds.add( instanceStatus.getInstanceId() );
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
      this.expectedRunningInstanceIds = expectedRunningInstanceIds;
    }

    @Override
    boolean shouldRun() {
      return !expectedRunningInstanceIds.isEmpty() || !pendingInstanceIds.isEmpty();
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

      try {
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
      //TODO:STEVE: Handle AccessPointNotFound if/when ELB service implements it
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
      final EucalyptusClient client = context.getEucalyptusClient();

      final DescribeKeyPairsType describeKeyPairsType
          = new DescribeKeyPairsType();
      describeKeyPairsType.getFilterSet().add( filter( "key-name", sshKey ) );

      client.dispatch( describeKeyPairsType, callback );
    }

    @Override
    void dispatchSuccess( final ActivityContext context,
                          final DescribeKeyPairsResponseType response ) {
      if ( response.getKeySet() == null || response.getKeySet().size() != 1 ) {
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
    private final String keyName;
    private final AtomicReference<List<String>> validationErrors = new AtomicReference<List<String>>(
        Collections.<String>emptyList()
    );

    ValidationScalingProcessTask( final OwnerFullName owner,
                                  final List<String> availabilityZones,
                                  final List<String> loadBalancerNames,
                                  final List<String> imageIds,
                                  @Nullable final String keyName,
                                  final List<String> securityGroups ) {
      super( UUID.randomUUID().toString() + "-validation", AutoScalingGroup.withOwner(owner), "Validate" );
      this.availabilityZones = availabilityZones;
      this.loadBalancerNames = loadBalancerNames;
      this.imageIds = imageIds;
      this.keyName = keyName;
      this.securityGroups = securityGroups;
    }

    @Override
    boolean shouldRun() {
      return
          !availabilityZones.isEmpty() ||
          !loadBalancerNames.isEmpty() ||
          !imageIds.isEmpty() ||
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
      final List<String> validationErrors = Lists.newArrayList();
      for ( final ValidationScalingActivityTask<?> task : tasks ) {
        validationErrors.addAll( task.getValidationErrors() );
      }
      this.validationErrors.set( ImmutableList.copyOf( validationErrors ) );
    }

    List<String> getValidationErrors() {
      return validationErrors.get();
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
