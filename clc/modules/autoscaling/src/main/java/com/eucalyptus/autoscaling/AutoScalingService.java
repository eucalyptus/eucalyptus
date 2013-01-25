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

import static com.eucalyptus.autoscaling.AutoScalingMetadata.LaunchConfigurationMetadata;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration;
import com.eucalyptus.autoscaling.configurations.LaunchConfigurations;
import com.eucalyptus.autoscaling.configurations.PersistenceLaunchConfigurations;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Numbers;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;

public class AutoScalingService {
  private static final Logger logger = Logger.getLogger( AutoScalingService.class );
  private final LaunchConfigurations launchConfigurations;
  
  public AutoScalingService() {
    this( new PersistenceLaunchConfigurations() );
  }

  protected AutoScalingService( final LaunchConfigurations launchConfigurations ) {
    this.launchConfigurations = launchConfigurations;
  }

  public DescribeAutoScalingGroupsResponseType describeAutoScalingGroups(DescribeAutoScalingGroupsType request) throws EucalyptusCloudException {
    DescribeAutoScalingGroupsResponseType reply = request.getReply( );
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
    } catch ( NoSuchMetadataException e ) {
      // so nothing to delete, move along      
    } catch ( Exception e ) {
      handleException( e );
    }    
    return reply;
  }

  public DescribePoliciesResponseType describePolicies(DescribePoliciesType request) throws EucalyptusCloudException {
    DescribePoliciesResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeScalingProcessTypesResponseType describeScalingProcessTypes(DescribeScalingProcessTypesType request) throws EucalyptusCloudException {
    DescribeScalingProcessTypesResponseType reply = request.getReply( );
    return reply;
  }

  public CreateAutoScalingGroupResponseType createAutoScalingGroup(CreateAutoScalingGroupType request) throws EucalyptusCloudException {
    CreateAutoScalingGroupResponseType reply = request.getReply( );
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

  public DescribeTerminationPolicyTypesResponseType describeTerminationPolicyTypes(DescribeTerminationPolicyTypesType request) throws EucalyptusCloudException {
    DescribeTerminationPolicyTypesResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeTagsResponseType describeTags(DescribeTagsType request) throws EucalyptusCloudException {
    DescribeTagsResponseType reply = request.getReply( );
    return reply;
  }

  public ExecutePolicyResponseType executePolicy(ExecutePolicyType request) throws EucalyptusCloudException {
    ExecutePolicyResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteTagsResponseType deleteTags(DeleteTagsType request) throws EucalyptusCloudException {
    DeleteTagsResponseType reply = request.getReply( );
    return reply;
  }

  public PutScalingPolicyResponseType putScalingPolicy(PutScalingPolicyType request) throws EucalyptusCloudException {
    PutScalingPolicyResponseType reply = request.getReply( );
    return reply;
  }

  public PutNotificationConfigurationResponseType putNotificationConfiguration(PutNotificationConfigurationType request) throws EucalyptusCloudException {
    PutNotificationConfigurationResponseType reply = request.getReply( );
    return reply;
  }

  public DeletePolicyResponseType deletePolicy(DeletePolicyType request) throws EucalyptusCloudException {
    DeletePolicyResponseType reply = request.getReply( );
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

  public SetInstanceHealthResponseType setInstanceHealth(SetInstanceHealthType request) throws EucalyptusCloudException {
    SetInstanceHealthResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeAutoScalingNotificationTypesResponseType describeAutoScalingNotificationTypes(DescribeAutoScalingNotificationTypesType request) throws EucalyptusCloudException {
    DescribeAutoScalingNotificationTypesResponseType reply = request.getReply( );
    return reply;
  }

  public CreateOrUpdateTagsResponseType createOrUpdateTags(CreateOrUpdateTagsType request) throws EucalyptusCloudException {
    CreateOrUpdateTagsResponseType reply = request.getReply( );
    return reply;
  }

  public SuspendProcessesResponseType suspendProcesses(SuspendProcessesType request) throws EucalyptusCloudException {
    SuspendProcessesResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeAutoScalingInstancesResponseType describeAutoScalingInstances(DescribeAutoScalingInstancesType request) throws EucalyptusCloudException {
    DescribeAutoScalingInstancesResponseType reply = request.getReply( );
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
              ctx.getUserFullName().asAccountFullName(),
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
      handleException( e );
    }

    return reply;
  }

  public DeleteAutoScalingGroupResponseType deleteAutoScalingGroup(DeleteAutoScalingGroupType request) throws EucalyptusCloudException {
    DeleteAutoScalingGroupResponseType reply = request.getReply( );
    return reply;
  }

  public DisableMetricsCollectionResponseType disableMetricsCollection(DisableMetricsCollectionType request) throws EucalyptusCloudException {
    DisableMetricsCollectionResponseType reply = request.getReply( );
    return reply;
  }

  public UpdateAutoScalingGroupResponseType updateAutoScalingGroup(UpdateAutoScalingGroupType request) throws EucalyptusCloudException {
    UpdateAutoScalingGroupResponseType reply = request.getReply( );
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
        AutoScalingMetadatas.filterPrivilegesById( request.launchConfigurationNames() );

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

  public DescribeAdjustmentTypesResponseType describeAdjustmentTypes(DescribeAdjustmentTypesType request) throws EucalyptusCloudException {
    DescribeAdjustmentTypesResponseType reply = request.getReply( );
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

  public SetDesiredCapacityResponseType setDesiredCapacity(SetDesiredCapacityType request) throws EucalyptusCloudException {
    SetDesiredCapacityResponseType reply = request.getReply( );
    return reply;
  }

  public TerminateInstanceInAutoScalingGroupResponseType terminateInstanceInAutoScalingGroup(TerminateInstanceInAutoScalingGroupType request) throws EucalyptusCloudException {
    TerminateInstanceInAutoScalingGroupResponseType reply = request.getReply( );
    return reply;
  }

  private static void handleException( final Exception e ) throws AutoScalingException {
    final AutoScalingException cause = Exceptions.findCause( e, AutoScalingException.class );
    if ( cause != null ) {
      throw cause;
    }

    logger.error( e, e );

    final InternalFailureException exception = new InternalFailureException( e.getMessage() );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );      
    }
    throw exception;
  }
}
