/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY without even the implied warranty of
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
package com.eucalyptus.autoscaling.common

import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import com.eucalyptus.component.ComponentId
import com.google.common.collect.Lists
import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping
import java.lang.reflect.Field

public class DescribeMetricCollectionTypesType extends AutoScalingMessage {
  public DescribeMetricCollectionTypesType() {  }
}
public class Alarm extends EucalyptusData {
  String alarmName
  String alarmARN
  public Alarm() {  }
}
public class MetricGranularityTypes extends EucalyptusData {
  public MetricGranularityTypes() {  }
  ArrayList<MetricGranularityType> member = new ArrayList<MetricGranularityType>()
}
public class DescribeAutoScalingNotificationTypesResponseType extends AutoScalingMessage {
  public DescribeAutoScalingNotificationTypesResponseType() {  }
  DescribeAutoScalingNotificationTypesResult describeAutoScalingNotificationTypesResult = new DescribeAutoScalingNotificationTypesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class LaunchConfigurationNames extends EucalyptusData {
  public LaunchConfigurationNames() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
@ComponentId.ComponentMessage(AutoScaling.class)
public class AutoScalingMessage extends BaseMessage {
  @Override
  def <TYPE extends BaseMessage> TYPE getReply() {
    TYPE type = super.getReply()
    try {
      Field responseMetadataField = type.class.getDeclaredField("responseMetadata")
      responseMetadataField.setAccessible( true ) 
      ((ResponseMetadata) responseMetadataField.get( type )).requestId = getCorrelationId()
    } catch ( Exception e ) {       
    }
    return type
  }
}
public class SuspendProcessesResponseType extends AutoScalingMessage {
  public SuspendProcessesResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class AutoScalingNotificationTypes extends EucalyptusData {
  public AutoScalingNotificationTypes() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class TerminateInstanceInAutoScalingGroupType extends AutoScalingMessage {
  String instanceId
  Boolean shouldDecrementDesiredCapacity
  public TerminateInstanceInAutoScalingGroupType() {  }
}
public class ErrorResponse extends AutoScalingMessage {
  String requestId
  public ErrorResponse() {  }
  ArrayList<Error> error = new ArrayList<Error>()
}
public class BlockDeviceMappings extends EucalyptusData {
  public BlockDeviceMappings() {  }
  public BlockDeviceMappings( Collection<BlockDeviceMappingType> mappings ) { 
    member.addAll( mappings )
  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<BlockDeviceMappingType> member = new ArrayList<BlockDeviceMappingType>()
}
public class LoadBalancerNames extends EucalyptusData {
  public LoadBalancerNames() {  }
  public LoadBalancerNames( Collection<String> names ) {
    if ( names != null ) member.addAll( names )
  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class PolicyNames extends EucalyptusData {
  public PolicyNames() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class DescribeTerminationPolicyTypesType extends AutoScalingMessage {
  public DescribeTerminationPolicyTypesType() {  }
}
public class DeleteTagsResponseType extends AutoScalingMessage {
  public DeleteTagsResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class SetInstanceHealthResponseType extends AutoScalingMessage {
  public SetInstanceHealthResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DeleteAutoScalingGroupType extends AutoScalingMessage {
  String autoScalingGroupName
  Boolean forceDelete
  public DeleteAutoScalingGroupType() {  }
}
public class DescribeNotificationConfigurationsType extends AutoScalingMessage {
  @HttpEmbedded
  AutoScalingGroupNames autoScalingGroupNames
  String nextToken
  Integer maxRecords
  public DescribeNotificationConfigurationsType() {  }
}
public class ScheduledUpdateGroupAction extends EucalyptusData {
  String autoScalingGroupName
  String scheduledActionName
  String scheduledActionARN
  Date time
  Date startTime
  Date endTime
  String recurrence
  Integer minSize
  Integer maxSize
  Integer desiredCapacity
  public ScheduledUpdateGroupAction() {  }
}
public class PutScheduledUpdateGroupActionResponseType extends AutoScalingMessage {
  public PutScheduledUpdateGroupActionResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class ProcessType extends EucalyptusData {
  String processName
  public ProcessType() {  }
}
public class TagDescription extends EucalyptusData {
  String resourceId
  String resourceType
  String key
  String value
  Boolean propagateAtLaunch
  public TagDescription() {  }
}
public class DeleteNotificationConfigurationType extends AutoScalingMessage {
  String autoScalingGroupName
  String topicARN
  public DeleteNotificationConfigurationType() {  }
}
public class ExecutePolicyType extends AutoScalingMessage {
  String autoScalingGroupName
  String policyName
  Boolean honorCooldown
  public ExecutePolicyType() {  }
}
public class AutoScalingInstanceDetails extends EucalyptusData {
  String instanceId
  String autoScalingGroupName
  String availabilityZone
  String lifecycleState
  String healthStatus
  String launchConfigurationName
  public AutoScalingInstanceDetails() {  }
}
public class DeletePolicyType extends AutoScalingMessage {
  String autoScalingGroupName
  String policyName
  public DeletePolicyType() {  }
}
public class DescribeAutoScalingGroupsResult extends EucalyptusData {
  AutoScalingGroupsType autoScalingGroups = new AutoScalingGroupsType()
  String nextToken
  public DescribeAutoScalingGroupsResult() {  }
}
public class CreateLaunchConfigurationResponseType extends AutoScalingMessage {
  public CreateLaunchConfigurationResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class TerminateInstanceInAutoScalingGroupResponseType extends AutoScalingMessage {
  public TerminateInstanceInAutoScalingGroupResponseType() {  }
  TerminateInstanceInAutoScalingGroupResult terminateInstanceInAutoScalingGroupResult = new TerminateInstanceInAutoScalingGroupResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeAutoScalingInstancesResponseType extends AutoScalingMessage {
  public DescribeAutoScalingInstancesResponseType() {  }
  DescribeAutoScalingInstancesResult describeAutoScalingInstancesResult = new DescribeAutoScalingInstancesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class PutNotificationConfigurationType extends AutoScalingMessage {
  String autoScalingGroupName
  String topicARN
  @HttpEmbedded
  AutoScalingNotificationTypes notificationTypes
  public PutNotificationConfigurationType() {  }
}
public class MetricCollectionTypes extends EucalyptusData {
  public MetricCollectionTypes() {  }
  ArrayList<MetricCollectionType> member = new ArrayList<MetricCollectionType>()
}
public class CreateAutoScalingGroupResponseType extends AutoScalingMessage {
  public CreateAutoScalingGroupResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DeleteNotificationConfigurationResponseType extends AutoScalingMessage {
  public DeleteNotificationConfigurationResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class TagDescriptionList extends EucalyptusData {
  public TagDescriptionList() {  }
  ArrayList<TagDescription> member = new ArrayList<TagDescription>()
}
public class DescribeTerminationPolicyTypesResult extends EucalyptusData {
  TerminationPolicies terminationPolicyTypes = new TerminationPolicies()
  public DescribeTerminationPolicyTypesResult() {  }
}
public class DescribeScalingProcessTypesResponseType extends AutoScalingMessage {
  public DescribeScalingProcessTypesResponseType() {  }
  DescribeScalingProcessTypesResult describeScalingProcessTypesResult = new DescribeScalingProcessTypesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class PutNotificationConfigurationResponseType extends AutoScalingMessage {
  public PutNotificationConfigurationResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class Activity extends EucalyptusData {
  String activityId
  String autoScalingGroupName
  String description
  String cause
  Date startTime
  Date endTime
  String statusCode
  String statusMessage
  Integer progress
  String details
  public Activity() {  }
}
public class SuspendedProcesses extends EucalyptusData {
  public SuspendedProcesses() {  }
  ArrayList<SuspendedProcess> member = new ArrayList<SuspendedProcess>()
}
public class InstanceMonitoring extends EucalyptusData {
  Boolean enabled
  public InstanceMonitoring() {  }
  public InstanceMonitoring( Boolean enabled ) {
    this.enabled = enabled  
  }
}
public class DescribeScheduledActionsType extends AutoScalingMessage {
  String autoScalingGroupName
  @HttpEmbedded
  ScheduledActionNames scheduledActionNames
  Date startTime
  Date endTime
  String nextToken
  Integer maxRecords
  public DescribeScheduledActionsType() {  }
}
public class Filter extends EucalyptusData {
  String name
  @HttpEmbedded
  Values values
  public Filter() {  }
}
public class ErrorDetail extends EucalyptusData {
  public ErrorDetail() {  }
}
public class Alarms extends EucalyptusData {
  public Alarms() {  }
  ArrayList<Alarm> member = new ArrayList<Alarm>()
}
public class DescribeAutoScalingInstancesResult extends EucalyptusData {
  AutoScalingInstances autoScalingInstances = new AutoScalingInstances()
  String nextToken
  public DescribeAutoScalingInstancesResult() {  }
}
public class DescribeLaunchConfigurationsType extends AutoScalingMessage {
  @HttpEmbedded
  LaunchConfigurationNames launchConfigurationNames = new LaunchConfigurationNames()
  String nextToken
  Integer maxRecords
  public DescribeLaunchConfigurationsType() {  }
  public List<String> launchConfigurationNames() {
    List<String> names = Lists.newArrayList()
    if ( launchConfigurationNames != null ) {
      names = launchConfigurationNames.getMember()  
    }
    return names
  }
}
public class DescribeMetricCollectionTypesResponseType extends AutoScalingMessage {
  public DescribeMetricCollectionTypesResponseType() {  }
  DescribeMetricCollectionTypesResult describeMetricCollectionTypesResult = new DescribeMetricCollectionTypesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class AutoScalingInstances extends EucalyptusData {
  public AutoScalingInstances() {  }
  ArrayList<AutoScalingInstanceDetails> member = new ArrayList<AutoScalingInstanceDetails>()
}
public class DescribeTagsType extends AutoScalingMessage {
  @HttpEmbedded
  Filters filters
  String nextToken
  Integer maxRecords
  public DescribeTagsType() {  }
}
public class AdjustmentType extends EucalyptusData {
  String adjustmentType
  public AdjustmentType() {  }
}
public class DeleteScheduledActionType extends AutoScalingMessage {
  String autoScalingGroupName
  String scheduledActionName
  public DeleteScheduledActionType() {  }
}
public class DisableMetricsCollectionResponseType extends AutoScalingMessage {
  public DisableMetricsCollectionResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class CreateAutoScalingGroupType extends AutoScalingMessage {
  String autoScalingGroupName
  String launchConfigurationName
  Integer minSize
  Integer maxSize
  Integer desiredCapacity
  Integer defaultCooldown
  @HttpEmbedded
  AvailabilityZones availabilityZones
  @HttpEmbedded
  LoadBalancerNames loadBalancerNames
  String healthCheckType
  Integer healthCheckGracePeriod
  String placementGroup
  @HttpParameterMapping(parameter="VPCZoneIdentifier")
  String vpcZoneIdentifier
  @HttpEmbedded
  TerminationPolicies terminationPolicies
  @HttpEmbedded
  Tags tags
  public CreateAutoScalingGroupType() {  }
  public Collection<String> availabilityZones() {
    return availabilityZones?.member
  }
  public Collection<String> loadBalancerNames() {
    return loadBalancerNames?.member
  }
  public Collection<String> terminationPolicies() {
    return terminationPolicies?.member
  }
}
public class DisableMetricsCollectionType extends AutoScalingMessage {
  String autoScalingGroupName
  @HttpEmbedded
  Metrics metrics
  public DisableMetricsCollectionType() {  }
}
public class DescribeAdjustmentTypesType extends AutoScalingMessage {
  public DescribeAdjustmentTypesType() {  }
}
public class TerminationPolicies extends EucalyptusData {
  public TerminationPolicies() {  }
  public TerminationPolicies( Collection<String> terminationPolicies ) { 
    if ( terminationPolicies != null ) member.addAll( terminationPolicies )
  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class NotificationConfiguration extends EucalyptusData {
  String autoScalingGroupName
  String topicARN
  String notificationType
  public NotificationConfiguration() {  }
}
public class DescribeTagsResult extends EucalyptusData {
  TagDescriptionList tags
  String nextToken
  public DescribeTagsResult() {  }
}
public class DescribeNotificationConfigurationsResponseType extends AutoScalingMessage {
  public DescribeNotificationConfigurationsResponseType() {  }
  DescribeNotificationConfigurationsResult describeNotificationConfigurationsResult = new DescribeNotificationConfigurationsResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class ScheduledActionNames extends EucalyptusData {
  public ScheduledActionNames() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class AvailabilityZones extends EucalyptusData {
  public AvailabilityZones() {  }
  public AvailabilityZones( Collection<String> zones ) { 
    if ( zones != null ) member.addAll( zones )
  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class DescribeScalingActivitiesResult extends EucalyptusData {
  Activities activities = new Activities()
  String nextToken
  public DescribeScalingActivitiesResult() {  }
}
public class DescribeAutoScalingNotificationTypesType extends AutoScalingMessage {
  public DescribeAutoScalingNotificationTypesType() {  }
}
public class Metrics extends EucalyptusData {
  public Metrics() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class DeleteScheduledActionResponseType extends AutoScalingMessage {
  public DeleteScheduledActionResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeNotificationConfigurationsResult extends EucalyptusData {
  NotificationConfigurations notificationConfigurations = new NotificationConfigurations()
  String nextToken
  public DescribeNotificationConfigurationsResult() {  }
}
public class EnableMetricsCollectionType extends AutoScalingMessage {
  String autoScalingGroupName
  @HttpEmbedded
  Metrics metrics
  String granularity
  public EnableMetricsCollectionType() {  }
}
public class EnableMetricsCollectionResponseType extends AutoScalingMessage {
  public EnableMetricsCollectionResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class PutScheduledUpdateGroupActionType extends AutoScalingMessage {
  String autoScalingGroupName
  String scheduledActionName
  Date time
  Date startTime
  Date endTime
  String recurrence
  Integer minSize
  Integer maxSize
  Integer desiredCapacity
  public PutScheduledUpdateGroupActionType() {  }
}
public class SuspendedProcess extends EucalyptusData {
  String processName
  String suspensionReason
  public SuspendedProcess() {  }
}
public class SecurityGroups extends EucalyptusData {
  public SecurityGroups() {  }
  public SecurityGroups( Collection<String> groups ) {
    member.addAll( groups )  
  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class NotificationConfigurations extends EucalyptusData {
  public NotificationConfigurations() {  }
  ArrayList<NotificationConfiguration> member = new ArrayList<NotificationConfiguration>()
}
public class DeleteLaunchConfigurationResponseType extends AutoScalingMessage {
  public DeleteLaunchConfigurationResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeScheduledActionsResponseType extends AutoScalingMessage {
  public DescribeScheduledActionsResponseType() {  }
  DescribeScheduledActionsResult describeScheduledActionsResult = new DescribeScheduledActionsResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class Filters extends EucalyptusData {
  public Filters() {  }
  @HttpEmbedded
  @HttpParameterMapping(parameter="member")
  ArrayList<Filter> member = new ArrayList<Filter>()
}
public class ResumeProcessesType extends AutoScalingMessage {
  String autoScalingGroupName
  @HttpEmbedded
  ProcessNames scalingProcesses
  public ResumeProcessesType() {  }
}
public class DescribeAdjustmentTypesResponseType extends AutoScalingMessage {
  public DescribeAdjustmentTypesResponseType() {  }
  DescribeAdjustmentTypesResult describeAdjustmentTypesResult = new DescribeAdjustmentTypesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class InstanceIds extends EucalyptusData {
  public InstanceIds() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class SuspendProcessesType extends AutoScalingMessage {
  String autoScalingGroupName
  @HttpEmbedded
  ProcessNames scalingProcesses
  public SuspendProcessesType() {  }
}
public class LaunchConfigurationsType extends EucalyptusData {
  public LaunchConfigurationsType() {  }
  ArrayList<LaunchConfigurationType> member = new ArrayList<LaunchConfigurationType>()
}
public class Instances extends EucalyptusData {
  public Instances() {  }
  ArrayList<Instance> member = new ArrayList<Instance>()
}
public class TerminateInstanceInAutoScalingGroupResult extends EucalyptusData {
  Activity activity
  public TerminateInstanceInAutoScalingGroupResult() {  }
}
public class DescribeScheduledActionsResult extends EucalyptusData {
  ScheduledUpdateGroupActions scheduledUpdateGroupActions
  String nextToken
  public DescribeScheduledActionsResult() {  }
}
public class DescribeAutoScalingInstancesType extends AutoScalingMessage {
  @HttpEmbedded
  InstanceIds instanceIds
  Integer maxRecords
  String nextToken
  public DescribeAutoScalingInstancesType() {  }
  public List<String> instanceIds() {
    List<String> names = Lists.newArrayList()
    if ( instanceIds != null ) {
      names = instanceIds.getMember()
    }
    return names
  }
}
public class DeleteTagsType extends AutoScalingMessage {
  @HttpEmbedded
  Tags tags
  public DeleteTagsType() {  }
}
public class UpdateAutoScalingGroupResponseType extends AutoScalingMessage {
  public UpdateAutoScalingGroupResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class EnabledMetric extends EucalyptusData {
  String metric
  String granularity
  public EnabledMetric() {  }
}
public class DescribePoliciesResponseType extends AutoScalingMessage {
  public DescribePoliciesResponseType() {  }
  DescribePoliciesResult describePoliciesResult = new DescribePoliciesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class Tag extends EucalyptusData {
  String resourceId
  String resourceType
  String key
  String value
  Boolean propagateAtLaunch
  public Tag() {  }
}
public class DescribeTagsResponseType extends AutoScalingMessage {
  public DescribeTagsResponseType() {  }
  DescribeTagsResult describeTagsResult = new DescribeTagsResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class ScheduledUpdateGroupActions extends EucalyptusData {
  public ScheduledUpdateGroupActions() {  }
  ArrayList<ScheduledUpdateGroupAction> member = new ArrayList<ScheduledUpdateGroupAction>()
}
public class DeletePolicyResponseType extends AutoScalingMessage {
  public DeletePolicyResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class Instance extends EucalyptusData {
  String instanceId
  String availabilityZone
  String lifecycleState
  String healthStatus
  String launchConfigurationName
  public Instance() {  }
}
public class ExecutePolicyResponseType extends AutoScalingMessage {
  public ExecutePolicyResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class ActivityIds extends EucalyptusData {
  public ActivityIds() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class MetricGranularityType extends EucalyptusData {
  String granularity
  public MetricGranularityType() {  }
}
public class AdjustmentTypes extends EucalyptusData {
  public AdjustmentTypes() {  }
  ArrayList<AdjustmentType> member = new ArrayList<AdjustmentType>()
}
public class PutScalingPolicyResponseType extends AutoScalingMessage {
  public PutScalingPolicyResponseType() {  }
  PutScalingPolicyResult putScalingPolicyResult = new PutScalingPolicyResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class Tags extends EucalyptusData {
  public Tags() {  }
  @HttpParameterMapping(parameter="member")
  @HttpEmbedded(multiple=true)
  ArrayList<Tag> member = new ArrayList<Tag>()
}
public class SetDesiredCapacityResponseType extends AutoScalingMessage {
  public SetDesiredCapacityResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeScalingActivitiesType extends AutoScalingMessage {
  @HttpEmbedded
  ActivityIds activityIds
  String autoScalingGroupName
  Integer maxRecords
  String nextToken
  public DescribeScalingActivitiesType() {  }
}
public class LaunchConfigurationType extends EucalyptusData {
  String launchConfigurationName
  String launchConfigurationARN
  String imageId
  String keyName
  SecurityGroups securityGroups
  String userData
  String instanceType
  String kernelId
  String ramdiskId
  BlockDeviceMappings blockDeviceMappings
  InstanceMonitoring instanceMonitoring
  String spotPrice
  String iamInstanceProfile
  Date createdTime
  Boolean ebsOptimized
  public LaunchConfigurationType() {  }
}
public class Processes extends EucalyptusData {
  public Processes() {  }
  ArrayList<ProcessType> member = new ArrayList<ProcessType>()
}
public class Ebs extends EucalyptusData {
  String snapshotId
  Integer volumeSize
  public Ebs() {  }
}
public class SetInstanceHealthType extends AutoScalingMessage {
  String instanceId
  String healthStatus
  Boolean shouldRespectGracePeriod
  public SetInstanceHealthType() {  }
}
public class UpdateAutoScalingGroupType extends AutoScalingMessage {
  String autoScalingGroupName
  String launchConfigurationName
  Integer minSize
  Integer maxSize
  Integer desiredCapacity
  Integer defaultCooldown
  @HttpEmbedded
  AvailabilityZones availabilityZones
  String healthCheckType
  Integer healthCheckGracePeriod
  String placementGroup
  @HttpParameterMapping(parameter="VPCZoneIdentifier")
  String vpcZoneIdentifier
  @HttpEmbedded
  TerminationPolicies terminationPolicies
  public UpdateAutoScalingGroupType() {  }
  public Collection<String> availabilityZones() {
    return availabilityZones?.member
  }
  public Collection<String> terminationPolicies() {
    return terminationPolicies?.member
  }  
}
public class DescribeMetricCollectionTypesResult extends EucalyptusData {
  MetricCollectionTypes metrics
  MetricGranularityTypes granularities
  public DescribeMetricCollectionTypesResult() {  }
}
public class BlockDeviceMappingType extends EucalyptusData {
  String virtualName
  String deviceName
  @HttpEmbedded
  Ebs ebs
  public BlockDeviceMappingType() {  }
  public BlockDeviceMappingType( String deviceName, String virtualName, String snapshotId, Integer volumeSize ) {
    this.deviceName = deviceName
    this.virtualName = virtualName
    if ( snapshotId != null || volumeSize != null ) {
      this.ebs = new Ebs( snapshotId: snapshotId, volumeSize: volumeSize )  
    }
  }
}
public class ScalingPolicies extends EucalyptusData {
  public ScalingPolicies() {  }
  ArrayList<ScalingPolicyType> member = new ArrayList<ScalingPolicyType>()
}
public class ResponseMetadata extends EucalyptusData {
  String requestId
  public ResponseMetadata() {  }
}
public class DescribeTerminationPolicyTypesResponseType extends AutoScalingMessage {
  public DescribeTerminationPolicyTypesResponseType() {  }
  DescribeTerminationPolicyTypesResult describeTerminationPolicyTypesResult = new DescribeTerminationPolicyTypesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeLaunchConfigurationsResult extends EucalyptusData {
  LaunchConfigurationsType launchConfigurations = new LaunchConfigurationsType()
  String nextToken
  public DescribeLaunchConfigurationsResult() {  }
}
public class DescribePoliciesResult extends EucalyptusData {
  ScalingPolicies scalingPolicies = new ScalingPolicies()
  String nextToken
  public DescribePoliciesResult() {  }
}
public class AutoScalingGroupsType extends EucalyptusData {
  public AutoScalingGroupsType() {  }
  ArrayList<AutoScalingGroupType> member = new ArrayList<AutoScalingGroupType>()
}
public class EnabledMetrics extends EucalyptusData {
  public EnabledMetrics() {  }
  ArrayList<EnabledMetric> member = new ArrayList<EnabledMetric>()
}
public class SetDesiredCapacityType extends AutoScalingMessage {
  String autoScalingGroupName
  Integer desiredCapacity
  Boolean honorCooldown
  public SetDesiredCapacityType() {  }
}
public class PutScalingPolicyType extends AutoScalingMessage {
  String autoScalingGroupName
  String policyName
  Integer scalingAdjustment
  String adjustmentType
  Integer cooldown
  Integer minAdjustmentStep
  public PutScalingPolicyType() {  }
}
public class DescribeAutoScalingGroupsResponseType extends AutoScalingMessage {
  public DescribeAutoScalingGroupsResponseType() {  }
  DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class AutoScalingGroupType extends EucalyptusData {
  String autoScalingGroupName
  String autoScalingGroupARN
  String launchConfigurationName
  Integer minSize
  Integer maxSize
  Integer desiredCapacity
  Integer defaultCooldown
  AvailabilityZones availabilityZones
  LoadBalancerNames loadBalancerNames
  String healthCheckType
  Integer healthCheckGracePeriod
  Instances instances
  Date createdTime
  SuspendedProcesses suspendedProcesses
  String placementGroup
  String vpcZoneIdentifier
  EnabledMetrics enabledMetrics
  String status
  TagDescriptionList tags
  TerminationPolicies terminationPolicies
  public AutoScalingGroupType() {  }
}
public class DeleteLaunchConfigurationType extends AutoScalingMessage {
  String launchConfigurationName
  public DeleteLaunchConfigurationType() {  }
}
public class DeleteAutoScalingGroupResponseType extends AutoScalingMessage {
  public DeleteAutoScalingGroupResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeScalingActivitiesResponseType extends AutoScalingMessage {
  public DescribeScalingActivitiesResponseType() {  }
  DescribeScalingActivitiesResult describeScalingActivitiesResult = new DescribeScalingActivitiesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class ScalingPolicyType extends EucalyptusData {
  String autoScalingGroupName
  String policyName
  Integer scalingAdjustment
  String adjustmentType
  Integer cooldown
  String policyARN
  Alarms alarms
  Integer minAdjustmentStep
  public ScalingPolicyType() {  }
}
public class AutoScalingGroupNames extends EucalyptusData {
  public AutoScalingGroupNames() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class Values extends EucalyptusData {
  public Values() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class Error extends EucalyptusData {
  String type
  String code
  String message
  public Error() {  }
  ErrorDetail detail = new ErrorDetail()
}
public class CreateOrUpdateTagsResponseType extends AutoScalingMessage {
  public CreateOrUpdateTagsResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class Activities extends EucalyptusData {
  public Activities() {  }
  ArrayList<Activity> member = new ArrayList<Activity>()
}
public class DescribePoliciesType extends AutoScalingMessage {
  String autoScalingGroupName
  @HttpEmbedded
  PolicyNames policyNames
  String nextToken
  Integer maxRecords
  public DescribePoliciesType() {  }
  public List<String> policyNames() {
    List<String> names = Lists.newArrayList()
    if ( policyNames != null ) {
      names = policyNames.getMember()
    }
    return names
  }
}
public class PutScalingPolicyResult extends EucalyptusData {
  String policyARN
  public PutScalingPolicyResult() {  }
}
public class ResumeProcessesResponseType extends AutoScalingMessage {
  public ResumeProcessesResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeAutoScalingGroupsType extends AutoScalingMessage {
  @HttpEmbedded
  AutoScalingGroupNames autoScalingGroupNames
  String nextToken
  Integer maxRecords
  public DescribeAutoScalingGroupsType() {  }
  public List<String> autoScalingGroupNames() {
    List<String> names = Lists.newArrayList()
    if ( autoScalingGroupNames != null ) {
      names = autoScalingGroupNames.getMember()
    }
    return names
  }  
}
public class CreateLaunchConfigurationType extends AutoScalingMessage {
  String launchConfigurationName
  String imageId
  String keyName
  @HttpEmbedded
  SecurityGroups securityGroups
  String userData
  String instanceType
  String kernelId
  String ramdiskId
  @HttpEmbedded
  BlockDeviceMappings blockDeviceMappings
  @HttpEmbedded
  InstanceMonitoring instanceMonitoring
  String spotPrice
  String iamInstanceProfile
  Boolean ebsOptimized
  public CreateLaunchConfigurationType() {  }
}
public class MetricCollectionType extends EucalyptusData {
  String metric
  public MetricCollectionType() {  }
}
public class DescribeLaunchConfigurationsResponseType extends AutoScalingMessage {
  public DescribeLaunchConfigurationsResponseType() {  }
  DescribeLaunchConfigurationsResult describeLaunchConfigurationsResult = new DescribeLaunchConfigurationsResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeScalingProcessTypesType extends AutoScalingMessage {
  public DescribeScalingProcessTypesType() {  }
}
public class CreateOrUpdateTagsType extends AutoScalingMessage {
  @HttpEmbedded
  Tags tags
  public CreateOrUpdateTagsType() {  }
}
public class ProcessNames extends EucalyptusData {
  public ProcessNames() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class DescribeAdjustmentTypesResult extends EucalyptusData {
  AdjustmentTypes adjustmentTypes
  public DescribeAdjustmentTypesResult() {  }
  public void setAdjustmentTypes( Collection<String> values ) {
    adjustmentTypes = new AdjustmentTypes( member: values.collect { 
      value -> new AdjustmentType( adjustmentType: value ) } )  
  }
}
public class DescribeScalingProcessTypesResult extends EucalyptusData {
  Processes processes
  public DescribeScalingProcessTypesResult() {  }
}
public class DescribeAutoScalingNotificationTypesResult extends EucalyptusData {
  AutoScalingNotificationTypes autoScalingNotificationTypes
  public DescribeAutoScalingNotificationTypesResult() {  }
}
