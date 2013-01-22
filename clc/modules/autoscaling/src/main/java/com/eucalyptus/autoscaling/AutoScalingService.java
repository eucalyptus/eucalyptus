package com.eucalyptus.autoscaling;

import com.eucalyptus.autoscaling.CreateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.CreateAutoScalingGroupType;
import com.eucalyptus.autoscaling.CreateLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.CreateLaunchConfigurationType;
import com.eucalyptus.autoscaling.CreateOrUpdateTagsResponseType;
import com.eucalyptus.autoscaling.CreateOrUpdateTagsType;
import com.eucalyptus.autoscaling.DeleteAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.DeleteAutoScalingGroupType;
import com.eucalyptus.autoscaling.DeleteLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.DeleteLaunchConfigurationType;
import com.eucalyptus.autoscaling.DeleteNotificationConfigurationResponseType;
import com.eucalyptus.autoscaling.DeleteNotificationConfigurationType;
import com.eucalyptus.autoscaling.DeletePolicyResponseType;
import com.eucalyptus.autoscaling.DeletePolicyType;
import com.eucalyptus.autoscaling.DeleteScheduledActionResponseType;
import com.eucalyptus.autoscaling.DeleteScheduledActionType;
import com.eucalyptus.autoscaling.DeleteTagsResponseType;
import com.eucalyptus.autoscaling.DeleteTagsType;
import com.eucalyptus.autoscaling.DescribeAdjustmentTypesResponseType;
import com.eucalyptus.autoscaling.DescribeAdjustmentTypesType;
import com.eucalyptus.autoscaling.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.DescribeAutoScalingGroupsType;
import com.eucalyptus.autoscaling.DescribeAutoScalingInstancesResponseType;
import com.eucalyptus.autoscaling.DescribeAutoScalingInstancesType;
import com.eucalyptus.autoscaling.DescribeAutoScalingNotificationTypesResponseType;
import com.eucalyptus.autoscaling.DescribeAutoScalingNotificationTypesType;
import com.eucalyptus.autoscaling.DescribeLaunchConfigurationsResponseType;
import com.eucalyptus.autoscaling.DescribeLaunchConfigurationsType;
import com.eucalyptus.autoscaling.DescribeMetricCollectionTypesResponseType;
import com.eucalyptus.autoscaling.DescribeMetricCollectionTypesType;
import com.eucalyptus.autoscaling.DescribeNotificationConfigurationsResponseType;
import com.eucalyptus.autoscaling.DescribeNotificationConfigurationsType;
import com.eucalyptus.autoscaling.DescribePoliciesResponseType;
import com.eucalyptus.autoscaling.DescribePoliciesType;
import com.eucalyptus.autoscaling.DescribeScalingActivitiesResponseType;
import com.eucalyptus.autoscaling.DescribeScalingActivitiesType;
import com.eucalyptus.autoscaling.DescribeScalingProcessTypesResponseType;
import com.eucalyptus.autoscaling.DescribeScalingProcessTypesType;
import com.eucalyptus.autoscaling.DescribeScheduledActionsResponseType;
import com.eucalyptus.autoscaling.DescribeScheduledActionsType;
import com.eucalyptus.autoscaling.DescribeTagsResponseType;
import com.eucalyptus.autoscaling.DescribeTagsType;
import com.eucalyptus.autoscaling.DescribeTerminationPolicyTypesResponseType;
import com.eucalyptus.autoscaling.DescribeTerminationPolicyTypesType;
import com.eucalyptus.autoscaling.DisableMetricsCollectionResponseType;
import com.eucalyptus.autoscaling.DisableMetricsCollectionType;
import com.eucalyptus.autoscaling.EnableMetricsCollectionResponseType;
import com.eucalyptus.autoscaling.EnableMetricsCollectionType;
import com.eucalyptus.autoscaling.ExecutePolicyResponseType;
import com.eucalyptus.autoscaling.ExecutePolicyType;
import com.eucalyptus.autoscaling.PutNotificationConfigurationResponseType;
import com.eucalyptus.autoscaling.PutNotificationConfigurationType;
import com.eucalyptus.autoscaling.PutScalingPolicyResponseType;
import com.eucalyptus.autoscaling.PutScalingPolicyType;
import com.eucalyptus.autoscaling.PutScheduledUpdateGroupActionResponseType;
import com.eucalyptus.autoscaling.PutScheduledUpdateGroupActionType;
import com.eucalyptus.autoscaling.ResumeProcessesResponseType;
import com.eucalyptus.autoscaling.ResumeProcessesType;
import com.eucalyptus.autoscaling.SetDesiredCapacityResponseType;
import com.eucalyptus.autoscaling.SetDesiredCapacityType;
import com.eucalyptus.autoscaling.SetInstanceHealthResponseType;
import com.eucalyptus.autoscaling.SetInstanceHealthType;
import com.eucalyptus.autoscaling.SuspendProcessesResponseType;
import com.eucalyptus.autoscaling.SuspendProcessesType;
import com.eucalyptus.autoscaling.TerminateInstanceInAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.TerminateInstanceInAutoScalingGroupType;
import com.eucalyptus.autoscaling.UpdateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.UpdateAutoScalingGroupType;
import com.eucalyptus.util.EucalyptusCloudException;


public class AutoScalingService {
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

  public DeleteLaunchConfigurationResponseType deleteLaunchConfiguration(DeleteLaunchConfigurationType request) throws EucalyptusCloudException {
    DeleteLaunchConfigurationResponseType reply = request.getReply( );
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

  public CreateLaunchConfigurationResponseType createLaunchConfiguration(CreateLaunchConfigurationType request) throws EucalyptusCloudException {
    CreateLaunchConfigurationResponseType reply = request.getReply( );
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
    DescribeLaunchConfigurationsResponseType reply = request.getReply( );
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

}
