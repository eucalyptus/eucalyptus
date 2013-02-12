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
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
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
import com.eucalyptus.util.TypeMappers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.CreateTagsResponseType;
import edu.ucsb.eucalyptus.msgs.CreateTagsType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesType;
import edu.ucsb.eucalyptus.msgs.Filter;
import edu.ucsb.eucalyptus.msgs.ReservationInfoType;
import edu.ucsb.eucalyptus.msgs.ResourceTag;
import edu.ucsb.eucalyptus.msgs.RunInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesItemType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType;

/**
 * Launches / pokes / times out activities.
 */
public class ActivityManager {
  private static final Logger logger = Logger.getLogger( ActivityManager.class );

  //  LaunchInstance TerminateInstance  // TODO:STEVE: activity types?
  
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
        int compareResult = group.getCapacity().compareTo( group.getDesiredCapacity() );  //TODO:STEVE: verify capacity against autoscaling instances here?
        if ( compareResult < 0 ) {
          runner.runTask( perhapsLaunchInstances( group, group.getDesiredCapacity() - group.getCapacity() ) );
        } else if ( compareResult > 0 ) {
          runner.runTask( perhapsTerminateInstances( group, group.getCapacity() - group.getDesiredCapacity() ) );
        }
      }
    } catch ( Exception e ) {
      logger.error( e, e );  
    }
    
    // Replace unhealthy
    try {
      for ( final AutoScalingGroup group : autoScalingGroups.listRequiringInstanceReplacement() ) {
        runner.runTask( perhapsReplaceInstances( group ) ) ;        
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }
    
    // Monitor instances
    try {
      for ( final AutoScalingGroup group : autoScalingGroups.listRequiringMonitoring( 10000L ) ) {
        runner.runTask( ec2Monitoring( group ) );
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }

    //TODO:STEVE: When do we delete old scaling activities? (retain 6 weeks of activities by default as per AWS)
    //TODO:STEVE: Do we need to find running instances with auto scaling tags that we are not tracking and terminate them?
  }
  
  @Nullable
  public ScalingActivity terminateInstances( final AutoScalingGroup group, 
                                             final Collection<AutoScalingInstance> instances ) {
    final String arn = group.getArn();
    final Set<String> arnSet = Sets.newHashSet( Iterables.transform( instances, AutoScalingInstances.groupArn() ) );
    if ( arnSet.size()!=1 || !arnSet.iterator().next().equals( arn ) ) {
      throw new IllegalArgumentException( "Instances for termination must belong to the given group." );
    }
    
    final ScalingActivity[] activityHolder = new ScalingActivity[1];
    runner.runTask( new TaskWithBackOff( arn, "UserTermination" ) {
      @Override
      boolean isScalingTask() {
        return false;
      }

      @Override
      void runTask() {
        logger.info("Running user terminate instances activity for " + group.getArn() );

        boolean processInitiated = false;
        try {
          final String userId = Accounts.lookupAccountById( group.getOwnerAccountNumber() )
              .lookupUserByName( User.ACCOUNT_ADMIN ).getUserId();

          final EucalyptusClient client = new EucalyptusClient( userId );
          final ScalingActivity activity = ScalingActivity.create( group, "UserTerminate" );
          activityHolder[0] = scalingActivities.save( activity );
          processInitiated = true;

          final List<String> instancesToTerminate = Lists.newArrayList();
          Iterables.addAll(
              instancesToTerminate,
              Iterables.transform( instances, AutoScalingInstances.instanceId()) );

          client.dispatch(
              terminateInstances( instancesToTerminate ),
              new Callback.Checked<TerminateInstancesResponseType>() {
                @Override
                public void fireException( final Throwable e ) {
                  // error, assume no instances terminated for now
                  failure();
                  logger.error( "Error terminating instances", e ); //TODO:STEVE: Remove termination failure logging and record in scaling activity details/description
                  setScalingActivityFinalStatus( ActivityStatusCode.Failed, activity );
                }

                @Override
                public void fire( final TerminateInstancesResponseType response ) {
                  success();
                  setScalingActivityFinalStatus( ActivityStatusCode.Successful, activity );
                }
              } );
        } catch ( final Exception e ) {
          logger.error( e, e );
        } finally {
          if ( !processInitiated ) {
            failure();
          }
        }
      }
    } );
    
    return activityHolder[0];
  }
  
  private TaskWithBackOff perhapsLaunchInstances( final AutoScalingGroup group,
                                                  final int launchCount ) {
    final String arn = group.getArn();
    return new TaskWithBackOff( arn, "Launch" ) {
      @Override
      void runTask() {
        if ( launchCount == 0 ) {
          success();
        }
        
        logger.info("Running launch instances activity for " + group.getArn() );
        
        //TODO:STEVE: decide on an AZ to launch instances into  
        final int attemptToLaunch = Math.min( maxLaunchIncrement, launchCount );
        boolean processInitiated = false;
        try {
          final String userId = Accounts.lookupAccountById( group.getOwnerAccountNumber() )
              .lookupUserByName( User.ACCOUNT_ADMIN ).getUserId();
          final EucalyptusClient client = new EucalyptusClient( userId );
          final ScalingActivity activity = ScalingActivity.create( group, "Launch" );
          scalingActivities.save( activity );
          processInitiated = true;

          client.dispatch( runInstances( group, attemptToLaunch ),
              new Callback.Checked<RunInstancesResponseType>() {
                @Override
                public void fireException( final Throwable e ) {
                  // error, assume no instances run for now
                  failure();
                  logger.error( "Error launching instances", e ); //TODO:STEVE: Remove launch failure logging and record in scaling activity details/description
                  setScalingActivityFinalStatus( ActivityStatusCode.Failed, activity );
                }

                @Override
                public void fire( final RunInstancesResponseType response ) {
                  success();
                  final List<String> instanceIds = Lists.newArrayList();
                  for ( final RunningInstancesItemType item : response.getRsvInfo().getInstancesSet() ) {
                    instanceIds.add( item.getInstanceId() );
                    final AutoScalingInstance instance = AutoScalingInstance.create(
                        group.getOwner(),
                        item.getInstanceId(),
                        item.getPlacement(),
                        group );
                    // instance.setCreationTimestamp( item.getLaunchTime() ); //TODO:STEVE: should we track the launch time? (should probably be distinct from creation timestamp)
                    try {
                      autoScalingInstances.save( instance );
                    } catch ( AutoScalingMetadataException e ) {
                      logger.error( e, e );
                    }
                  }

                  try {
                    autoScalingGroups.update( group.getOwner(), group.getAutoScalingGroupName(), new Callback<AutoScalingGroup>(){
                      @Override
                      public void fire( final AutoScalingGroup autoScalingGroup ) {
                        autoScalingGroup.setCapacity( autoScalingGroup.getCapacity() + instanceIds.size() );
                      }
                    } );
                  } catch ( AutoScalingMetadataException e ) {
                    logger.error( e, e );
                  }
                  setScalingActivityFinalStatus( ActivityStatusCode.Successful, activity );

                  client.dispatch( tagInstances( instanceIds, group.getAutoScalingGroupName() ), new Failure<CreateTagsResponseType>(){
                    @Override
                    public void fireException( final Throwable e ) {
                      logger.error( e, e );
                    }
                  } );
                }
              } );
        } catch ( final Exception e ) {
          logger.error( e, e );
        } finally {
          if ( !processInitiated ) {
            failure();
          }
        }
      }
    };    
  }

  private TaskWithBackOff terminateInstances( final AutoScalingGroup group,
                                              final List<String> instanceIds,
                                              final boolean replace ) {
    final String arn = group.getArn();    
    return new TaskWithBackOff( arn, "Terminate" ) {
      private final AtomicInteger terminated = new AtomicInteger(0);

      @Override
      TaskWithBackOff onSuccess() {
        return replace ? 
            perhapsLaunchInstances( group, terminated.get() ) :
            null;
      }

      @Override
      void runTask() {
        if ( instanceIds.isEmpty() ) {
          success();          
        }
        
        logger.info("Running terminate instances activity for " + group.getArn() );

        boolean processInitiated = false;
        try {
          final String userId = Accounts.lookupAccountById( group.getOwnerAccountNumber() )
              .lookupUserByName( User.ACCOUNT_ADMIN ).getUserId();
          final List<AutoScalingInstance> currentInstances =
              autoScalingInstances.listByGroup( group.getOwner(), group.getAutoScalingGroupName() );

          final EucalyptusClient client = new EucalyptusClient( userId );
          final ScalingActivity activity = ScalingActivity.create( group, "Terminate" );
          scalingActivities.save( activity );
          processInitiated = true;

          final AtomicInteger terminated = this.terminated;
          client.dispatch(
              terminateInstances( instanceIds ),
              new Callback.Checked<TerminateInstancesResponseType>() {
                @Override
                public void fireException( final Throwable e ) {
                  // error, assume no instances terminated for now
                  failure();
                  logger.error( "Error terminating instances", e ); //TODO:STEVE: Remove termination failure logging and record in scaling activity details/description
                  setScalingActivityFinalStatus( ActivityStatusCode.Failed, activity );
                }

                @Override
                public void fire( final TerminateInstancesResponseType response ) {
                  try {
                    int terminatedCount = 0;
                    for ( final TerminateInstancesItemType item : response.getInstancesSet() ) {
                      terminatedCount++;
                      try {
                        final AutoScalingInstance instance = autoScalingInstances.lookup(
                            group.getOwner(),
                            item.getInstanceId() );
                        autoScalingInstances.delete( instance );
                      } catch ( AutoScalingMetadataNotFoundException e ) {
                        // no need to delete it then
                      } catch ( AutoScalingMetadataException e ) {
                        logger.error( e, e );
                      }
                    }
  
                    try {
                      terminated.set( terminatedCount );
                      autoScalingGroups.update( group.getOwner(), group.getAutoScalingGroupName(), new Callback<AutoScalingGroup>(){
                        @Override
                        public void fire( final AutoScalingGroup autoScalingGroup ) {
                          autoScalingGroup.setCapacity( currentInstances.size() - terminated.get() );
                        }
                      } );
                    } catch ( AutoScalingMetadataException e ) {
                      logger.error( e, e );
                    }
                    setScalingActivityFinalStatus( ActivityStatusCode.Successful, activity );
                  } finally {
                    success();
                  }
                }
              } );
        } catch ( final Exception e ) {
          logger.error( e, e );
        } finally {
          if ( !processInitiated ) {
            failure();
          }
        }
      }
    };  
  }
  
  private TaskWithBackOff perhapsTerminateInstances( final AutoScalingGroup group,
                                                     final int terminateCount ) {
    final List<String> instancesToTerminate = Lists.newArrayList();
    try {  
      final List<AutoScalingInstance> currentInstances =
          autoScalingInstances.listByGroup( group.getOwner(), group.getAutoScalingGroupName() );
      if ( currentInstances.size() == terminateCount ) {
        Iterables.addAll(
            instancesToTerminate,
            Iterables.transform( currentInstances, AutoScalingInstances.instanceId()) );
      } else {
        final List<AutoScalingInstance> remainingInstances = Lists.newArrayList( currentInstances );
        for ( int i=0; i<terminateCount && remainingInstances.size()>=1; i++ ) {
          final AutoScalingInstance instanceForTermination =
              TerminationPolicyType.selectForTermination( group.getTerminationPolicies(), remainingInstances );
          remainingInstances.remove( instanceForTermination );
          instancesToTerminate.add( instanceForTermination.getInstanceId() );
        }
      }
    } catch ( final Exception e ) {
      logger.error( e, e );
    }
    return terminateInstances( group, instancesToTerminate, false );
  }

  private TaskWithBackOff perhapsReplaceInstances( final AutoScalingGroup group ) {
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
    return terminateInstances( group, instancesToTerminate, true );
  }
  
  private TaskWithBackOff ec2Monitoring( final AutoScalingGroup group ) {
    final String arn = group.getArn();
    return new TaskWithBackOff( arn, "Monitor" ) {
      @Override
      void runTask() {
        logger.info("Running EC2 monitoring activity for " + group.getArn() );

        boolean processInitiated = false;
        try {
          final String userId = Accounts.lookupAccountById( group.getOwnerAccountNumber() )
              .lookupUserByName( User.ACCOUNT_ADMIN ).getUserId();

          final EucalyptusClient client = new EucalyptusClient( userId );
          final ScalingActivity activity = ScalingActivity.create( group, "Monitor" );
          scalingActivities.save( activity );
          processInitiated = true;

          client.dispatch(
              monitorInstances( group.getAutoScalingGroupName() ),
              new Callback.Checked<DescribeInstancesResponseType>() {
                @Override
                public void fireException( final Throwable e ) {
                  // error, assume everything is ok for now
                  failure();
                  logger.error( "Error monitoring instances", e ); //TODO:STEVE: Remove monitoring failure logging and record in scaling activity details/description
                  setScalingActivityFinalStatus( ActivityStatusCode.Failed, activity );
                }

                @Override
                public void fire( final DescribeInstancesResponseType response ) {
                  success();
                  
                  final List<String> runningInstanceIds = Lists.newArrayList();
                  for ( final ReservationInfoType reservation : response.getReservationSet() ){
                    for ( final RunningInstancesItemType instance : reservation.getInstancesSet() ) {
                      runningInstanceIds.add( instance.getInstanceId() );
                    }
                  }

                  try {
                    autoScalingInstances.markMissingInstancesUnhealthy( group, runningInstanceIds );
                  } catch ( AutoScalingMetadataException e ) {
                    logger.error( e, e );
                  }
                  setScalingActivityFinalStatus( ActivityStatusCode.Successful, activity );
                }
              } );
        } catch ( final Exception e ) {
          logger.error( e, e );
        } finally {
          if ( !processInitiated ) {
            failure();
          }
        }
      }
    };
  }
  
  private RunInstancesType runInstances( final AutoScalingGroup group, 
                                         final int attemptToLaunch ) {
    final LaunchConfiguration launchConfiguration = group.getLaunchConfiguration();
    final RunInstancesType runInstances = TypeMappers.transform( launchConfiguration, RunInstancesType.class );
    runInstances.setAvailabilityZone( Iterables.getFirst( group.getAvailabilityZones(), null ) );
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
  
  private DescribeInstancesType monitorInstances( final String autoScalingGroupName ) {
    final DescribeInstancesType describeInstancesType = new DescribeInstancesType();
    describeInstancesType.getFilterSet().add( filter( "tag:aws:autoscaling:groupName", autoScalingGroupName ) );    
    describeInstancesType.getFilterSet().add( filter( "instance-state-name", "running" ) );    
    return describeInstancesType;
  }
  
  private Filter filter( final String name, final String value ) {
    final Filter filter = new Filter();
    filter.setName( name );
    filter.getValueSet().add( value );
    return filter;
  }

  private void setScalingActivityFinalStatus( final ActivityStatusCode status, final ScalingActivity activity ) {
    try {
      scalingActivities.update( activity.getOwner(),
          activity.getActivityId(),
          new Callback<ScalingActivity>(){
            @Override
            public void fire( final ScalingActivity input ) {
              input.setActivityStatusCode( status );
              input.setEndTime( new Date() );
            }
          } );
    } catch ( AutoScalingMetadataException e ) {
      logger.error( e, e );
    }
  }

  /**
   * If scaling activities are not updated for some time we will fail them.
   * 
   * Activities should not require this cleanup, this is an error case.
   */
  private void timeoutScalingActivities( ) {
    try {
      for ( final ScalingActivity activity : scalingActivities.list( null ) ) {
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
