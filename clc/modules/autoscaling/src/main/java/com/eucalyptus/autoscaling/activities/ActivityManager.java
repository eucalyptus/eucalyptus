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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.autoscaling.groups.AutoScalingGroups;
import com.eucalyptus.autoscaling.groups.PersistenceAutoScalingGroups;
import com.eucalyptus.autoscaling.groups.TerminationPolicyType;
import com.eucalyptus.autoscaling.instances.AutoScalingInstance;
import com.eucalyptus.autoscaling.instances.AutoScalingInstances;
import com.eucalyptus.autoscaling.instances.PersistenceAutoScalingInstances;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataNotFoundException;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
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
import edu.ucsb.eucalyptus.msgs.CreateTagsResponseType;
import edu.ucsb.eucalyptus.msgs.CreateTagsType;
import edu.ucsb.eucalyptus.msgs.DescribeInstanceStatusResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstanceStatusType;
import edu.ucsb.eucalyptus.msgs.Filter;
import edu.ucsb.eucalyptus.msgs.InstanceStatusItemType;
import edu.ucsb.eucalyptus.msgs.ResourceTag;
import edu.ucsb.eucalyptus.msgs.RunInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;
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
        runTask( new MonitoringScalingProcessTask(
            group,
            Lists.newArrayList( Iterables.transform( autoScalingInstances.listByGroup( group ), instanceId() ) ) ) );
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
            targetZones = group.getAvailabilityZones();
          } else {
            targetZones = unwantedZones;
          }
        } else {
          targetZones = group.getAvailabilityZones();
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
                                       final String autoScalingGroupName ) {
    final CreateTagsType createTags = new CreateTagsType();
    createTags.getTagSet().add( new ResourceTag( "aws:autoscaling:groupName", autoScalingGroupName ) );
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
    } catch ( EucalyptusClient.EucalyptusClientException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  Supplier<String> userIdSupplier( final String accountNumber ) {
    return new Supplier<String>(){
      @Override
      public String get() {
        try {
          return Accounts.lookupAccountById( accountNumber )
              .lookupUserByName( User.ACCOUNT_ADMIN ).getUserId();
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    };
  }


  private interface ActivityContext {
    String getUserId();
    EucalyptusClient getEucalyptusClient();
  }

  private abstract class ScalingActivityTask<RES extends BaseMessage> {
    private volatile ScalingActivity activity;
    private final boolean persist;
    private volatile boolean dispatched = false;

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
        dispatched = true;
        return future;
      } catch ( Exception e ) {
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

    void failIfNotDispatched() {
      if ( !dispatched ) {
        setActivityFinalStatus( ActivityStatusCode.Failed );
      }
    }

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

    ScalingProcessTask( final AutoScalingGroup group,
                        final String activity ) {
      super( group.getArn(), activity );
      this.group = group;
      this.activity = activity;
      this.userIdSupplier = Suppliers.memoize( userIdSupplier( group.getOwnerAccountNumber() ) );
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
        for ( final ScalingActivityTask activity : activities ) {
          try {
            activity.failIfNotDispatched();
          } catch ( final Exception e ) {
            logger.error( e, e );
          }
        }
        if ( dispatchFutures.isEmpty() ) {
          failure();
        } else {
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
              } else {
                failure();
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

      getEucalyptusClient().dispatch( tagInstances( instanceIds, getGroup().getAutoScalingGroupName() ), new Callback.Failure<CreateTagsResponseType>() {
        @Override
        public void fireException( final Throwable e ) {
          logger.error( e, e );
        }
      } );
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
      terminated = true;
      try {
        // We ignore the response since we only requested termination of a
        // single instance. The response would be empty if the instance was
        // already terminated.
        final AutoScalingInstance instance = autoScalingInstances.lookup(
            getOwner(),
            instanceId );
        autoScalingInstances.delete( instance );
      } catch ( AutoScalingMetadataNotFoundException e ) {
        // no need to delete it then
      } catch ( AutoScalingMetadataException e ) {
        logger.error( e, e );
      }
      setActivityFinalStatus( ActivityStatusCode.Successful );
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

      for ( final String instanceId : instanceIds ) {
        activities.add( new TerminateInstanceScalingActivityTask( newActivity(), instanceId ) );
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
    private final List<String> instanceIds;

    MonitoringScalingProcessTask( final AutoScalingGroup group,
                                  final List<String> instanceIds ) {
      super( group, "Monitor" );
      this.instanceIds = instanceIds;
    }

    @Override
    boolean shouldRun() {
      return true;
    }

    @Override
    List<MonitoringScalingActivityTask> buildActivityTasks() throws AutoScalingMetadataException {
      return Collections.singletonList( new MonitoringScalingActivityTask( newActivity(), instanceIds ) );
    }

    @Override
    void partialSuccess( final List<MonitoringScalingActivityTask> tasks ) {
      final List<String> instanceIds = Lists.newArrayList();
      for ( final MonitoringScalingActivityTask task : tasks ) {
        instanceIds.addAll( task.getHealthyInstanceIds() );
      }

      try {
        autoScalingInstances.markMissingInstancesUnhealthy( getGroup(), instanceIds );
      } catch ( AutoScalingMetadataException e ) {
        logger.error( e, e );
      }
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
