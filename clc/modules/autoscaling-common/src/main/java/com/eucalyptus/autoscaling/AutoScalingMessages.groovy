
package com.eucalyptus.autoscaling;
import java.util.Date;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.math.BigInteger;
import java.util.ArrayList
import com.eucalyptus.component.ComponentId;



public class DescribeMetricCollectionTypesType extends AutoScalingMessage {
  public DescribeMetricCollectionTypesType() {  }
}
public class Alarm extends EucalyptusData {
  String alarmName;
  String alarmARN;
  public Alarm() {  }
}
public class MetricGranularityTypes extends EucalyptusData {
  public MetricGranularityTypes() {  }
  ArrayList<MetricGranularityType> member = new ArrayList<MetricGranularityType>();
}
public class DescribeAutoScalingNotificationTypesResponseType extends AutoScalingMessage {
  public DescribeAutoScalingNotificationTypesResponseType() {  }
  DescribeAutoScalingNotificationTypesResult describeAutoScalingNotificationTypesResult = new DescribeAutoScalingNotificationTypesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class LaunchConfigurationNames extends EucalyptusData {
  public LaunchConfigurationNames() {  }
  ArrayList<String> member = new ArrayList<String>();
}
@ComponentId.ComponentMessage(AutoScaling.class)
public class AutoScalingMessage extends BaseMessage {
  @Override
  def <TYPE extends BaseMessage> TYPE getReply() {
    TYPE type = super.getReply()
    if (type.properties.containsKey("responseMetadata")) {
      ((ResponseMetadata) type.properties.get("responseMetadata")).requestId = getCorrelationId();
    }
    return type
  }
}
public class SuspendProcessesResponseType extends AutoScalingMessage {
  public SuspendProcessesResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class AutoScalingNotificationTypes extends AutoScalingMessage {
  public AutoScalingNotificationTypes() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class TerminateInstanceInAutoScalingGroupType extends AutoScalingMessage {
  String instanceId;
  Boolean shouldDecrementDesiredCapacity;
  public TerminateInstanceInAutoScalingGroupType() {  }
}
public class ErrorResponse extends AutoScalingMessage {
  String requestId;
  public ErrorResponse() {  }
  ArrayList<Error> error = new ArrayList<Error>();
}
public class BlockDeviceMappings extends EucalyptusData {
  public BlockDeviceMappings() {  }
  ArrayList<BlockDeviceMapping> member = new ArrayList<BlockDeviceMapping>();
}
public class LoadBalancerNames extends EucalyptusData {
  public LoadBalancerNames() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class PolicyNames extends EucalyptusData {
  public PolicyNames() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class DescribeTerminationPolicyTypesType extends AutoScalingMessage {
  public DescribeTerminationPolicyTypesType() {  }
}
public class DeleteTagsResponseType extends AutoScalingMessage {
  public DeleteTagsResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class SetInstanceHealthResponseType extends AutoScalingMessage {
  public SetInstanceHealthResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DeleteAutoScalingGroupType extends AutoScalingMessage {
  String autoScalingGroupName;
  Boolean forceDelete;
  public DeleteAutoScalingGroupType() {  }
}
public class DescribeNotificationConfigurationsType extends AutoScalingMessage {
  AutoScalingGroupNames autoScalingGroupNames;
  String nextToken;
  BigInteger maxRecords;
  public DescribeNotificationConfigurationsType() {  }
}
public class ScheduledUpdateGroupAction extends AutoScalingMessage {
  String autoScalingGroupName;
  String scheduledActionName;
  String scheduledActionARN;
  Date time;
  Date startTime;
  Date endTime;
  String recurrence;
  BigInteger minSize;
  BigInteger maxSize;
  BigInteger desiredCapacity;
  public ScheduledUpdateGroupAction() {  }
}
public class PutScheduledUpdateGroupActionResponseType extends AutoScalingMessage {
  public PutScheduledUpdateGroupActionResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ProcessType extends EucalyptusData {
  String processName;
  public ProcessType() {  }
}
public class TagDescription extends EucalyptusData {
  String resourceId;
  String resourceType;
  String key;
  String value;
  Boolean propagateAtLaunch;
  public TagDescription() {  }
}
public class DeleteNotificationConfigurationType extends AutoScalingMessage {
  String autoScalingGroupName;
  String topicARN;
  public DeleteNotificationConfigurationType() {  }
}
public class ExecutePolicyType extends AutoScalingMessage {
  String autoScalingGroupName;
  String policyName;
  Boolean honorCooldown;
  public ExecutePolicyType() {  }
}
public class AutoScalingInstanceDetails extends EucalyptusData {
  String instanceId;
  String autoScalingGroupName;
  String availabilityZone;
  String lifecycleState;
  String healthStatus;
  String launchConfigurationName;
  public AutoScalingInstanceDetails() {  }
}
public class DeletePolicyType extends AutoScalingMessage {
  String autoScalingGroupName;
  String policyName;
  public DeletePolicyType() {  }
}
public class DescribeAutoScalingGroupsResult extends EucalyptusData {
  AutoScalingGroups autoScalingGroups = new AutoScalingGroups();
  String nextToken;
  public DescribeAutoScalingGroupsResult() {  }
}
public class CreateLaunchConfigurationResponseType extends AutoScalingMessage {
  public CreateLaunchConfigurationResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class TerminateInstanceInAutoScalingGroupResponseType extends AutoScalingMessage {
  public TerminateInstanceInAutoScalingGroupResponseType() {  }
  TerminateInstanceInAutoScalingGroupResult terminateInstanceInAutoScalingGroupResult = new TerminateInstanceInAutoScalingGroupResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeAutoScalingInstancesResponseType extends AutoScalingMessage {
  public DescribeAutoScalingInstancesResponseType() {  }
  DescribeAutoScalingInstancesResult describeAutoScalingInstancesResult = new DescribeAutoScalingInstancesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class PutNotificationConfigurationType extends AutoScalingMessage {
  String autoScalingGroupName;
  String topicARN;
  AutoScalingNotificationTypes notificationTypes;
  public PutNotificationConfigurationType() {  }
}
public class MetricCollectionTypes extends AutoScalingMessage {
  public MetricCollectionTypes() {  }
  ArrayList<MetricCollectionType> member = new ArrayList<MetricCollectionType>();
}
public class CreateAutoScalingGroupResponseType extends AutoScalingMessage {
  public CreateAutoScalingGroupResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DeleteNotificationConfigurationResponseType extends AutoScalingMessage {
  public DeleteNotificationConfigurationResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class TagDescriptionList extends EucalyptusData {
  public TagDescriptionList() {  }
  ArrayList<TagDescription> member = new ArrayList<TagDescription>();
}
public class DescribeTerminationPolicyTypesResult extends EucalyptusData {
  TerminationPolicies terminationPolicyTypes;
  public DescribeTerminationPolicyTypesResult() {  }
}
public class DescribeScalingProcessTypesResponseType extends AutoScalingMessage {
  public DescribeScalingProcessTypesResponseType() {  }
  DescribeScalingProcessTypesResult describeScalingProcessTypesResult = new DescribeScalingProcessTypesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class PutNotificationConfigurationResponseType extends AutoScalingMessage {
  public PutNotificationConfigurationResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class Activity extends EucalyptusData {
  String activityId;
  String autoScalingGroupName;
  String description;
  String cause;
  Date startTime;
  Date endTime;
  String statusCode;
  String statusMessage;
  BigInteger progress;
  String details;
  public Activity() {  }
}
public class SuspendedProcesses extends EucalyptusData {
  public SuspendedProcesses() {  }
  ArrayList<SuspendedProcess> member = new ArrayList<SuspendedProcess>();
}
public class InstanceMonitoring extends EucalyptusData {
  Boolean enabled;
  public InstanceMonitoring() {  }
}
public class DescribeScheduledActionsType extends AutoScalingMessage {
  String autoScalingGroupName;
  ScheduledActionNames scheduledActionNames;
  Date startTime;
  Date endTime;
  String nextToken;
  BigInteger maxRecords;
  public DescribeScheduledActionsType() {  }
}
public class Filter extends EucalyptusData {
  String name;
  Values values;
  public Filter() {  }
}
public class ErrorDetail extends EucalyptusData {
  public ErrorDetail() {  }
}
public class Alarms extends EucalyptusData {
  public Alarms() {  }
  ArrayList<Alarm> member = new ArrayList<Alarm>();
}
public class DescribeAutoScalingInstancesResult extends EucalyptusData {
  AutoScalingInstances autoScalingInstances;
  String nextToken;
  public DescribeAutoScalingInstancesResult() {  }
}
public class DescribeLaunchConfigurationsType extends AutoScalingMessage {
  LaunchConfigurationNames launchConfigurationNames;
  String nextToken;
  BigInteger maxRecords;
  public DescribeLaunchConfigurationsType() {  }
}
public class DescribeMetricCollectionTypesResponseType extends AutoScalingMessage {
  public DescribeMetricCollectionTypesResponseType() {  }
  DescribeMetricCollectionTypesResult describeMetricCollectionTypesResult = new DescribeMetricCollectionTypesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class AutoScalingInstances extends AutoScalingMessage {
  public AutoScalingInstances() {  }
  ArrayList<AutoScalingInstanceDetails> member = new ArrayList<AutoScalingInstanceDetails>();
}
public class DescribeTagsType extends AutoScalingMessage {
  Filters filters;
  String nextToken;
  BigInteger maxRecords;
  public DescribeTagsType() {  }
}
public class AdjustmentType extends EucalyptusData {
  String adjustmentType;
  public AdjustmentType() {  }
}
public class DeleteScheduledActionType extends AutoScalingMessage {
  String autoScalingGroupName;
  String scheduledActionName;
  public DeleteScheduledActionType() {  }
}
public class DisableMetricsCollectionResponseType extends AutoScalingMessage {
  public DisableMetricsCollectionResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class CreateAutoScalingGroupType extends AutoScalingMessage {
  String autoScalingGroupName;
  String launchConfigurationName;
  BigInteger minSize;
  BigInteger maxSize;
  BigInteger desiredCapacity;
  BigInteger defaultCooldown;
  AvailabilityZones availabilityZones;
  LoadBalancerNames loadBalancerNames;
  String healthCheckType;
  BigInteger healthCheckGracePeriod;
  String placementGroup;
  String vpcZoneIdentifier;
  TerminationPolicies terminationPolicies;
  Tags tags;
  public CreateAutoScalingGroupType() {  }
}
public class DisableMetricsCollectionType extends AutoScalingMessage {
  String autoScalingGroupName;
  Metrics metrics;
  public DisableMetricsCollectionType() {  }
}
public class DescribeAdjustmentTypesType extends AutoScalingMessage {
  public DescribeAdjustmentTypesType() {  }
}
public class TerminationPolicies extends EucalyptusData {
  public TerminationPolicies() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class NotificationConfiguration extends AutoScalingMessage {
  String autoScalingGroupName;
  String topicARN;
  String notificationType;
  public NotificationConfiguration() {  }
}
public class DescribeTagsResult extends EucalyptusData {
  TagDescriptionList tags;
  String nextToken;
  public DescribeTagsResult() {  }
}
public class DescribeNotificationConfigurationsResponseType extends AutoScalingMessage {
  public DescribeNotificationConfigurationsResponseType() {  }
  DescribeNotificationConfigurationsResult describeNotificationConfigurationsResult = new DescribeNotificationConfigurationsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ScheduledActionNames extends EucalyptusData {
  public ScheduledActionNames() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class AvailabilityZones extends EucalyptusData {
  public AvailabilityZones() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class DescribeScalingActivitiesResult extends EucalyptusData {
  Activities activities = new Activities();
  String nextToken;
  public DescribeScalingActivitiesResult() {  }
}
public class DescribeAutoScalingNotificationTypesType extends AutoScalingMessage {
  public DescribeAutoScalingNotificationTypesType() {  }
}
public class Metrics extends EucalyptusData {
  public Metrics() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class DeleteScheduledActionResponseType extends AutoScalingMessage {
  public DeleteScheduledActionResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeNotificationConfigurationsResult extends EucalyptusData {
  NotificationConfigurations notificationConfigurations = new NotificationConfigurations();
  String nextToken;
  public DescribeNotificationConfigurationsResult() {  }
}
public class EnableMetricsCollectionType extends AutoScalingMessage {
  String autoScalingGroupName;
  Metrics metrics;
  String granularity;
  public EnableMetricsCollectionType() {  }
}
public class EnableMetricsCollectionResponseType extends AutoScalingMessage {
  public EnableMetricsCollectionResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class PutScheduledUpdateGroupActionType extends AutoScalingMessage {
  String autoScalingGroupName;
  String scheduledActionName;
  Date time;
  Date startTime;
  Date endTime;
  String recurrence;
  BigInteger minSize;
  BigInteger maxSize;
  BigInteger desiredCapacity;
  public PutScheduledUpdateGroupActionType() {  }
}
public class SuspendedProcess extends EucalyptusData {
  String processName;
  String suspensionReason;
  public SuspendedProcess() {  }
}
public class SecurityGroups extends EucalyptusData {
  public SecurityGroups() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class NotificationConfigurations extends AutoScalingMessage {
  public NotificationConfigurations() {  }
  ArrayList<NotificationConfiguration> member = new ArrayList<NotificationConfiguration>();
}
public class DeleteLaunchConfigurationResponseType extends AutoScalingMessage {
  public DeleteLaunchConfigurationResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeScheduledActionsResponseType extends AutoScalingMessage {
  public DescribeScheduledActionsResponseType() {  }
  DescribeScheduledActionsResult describeScheduledActionsResult = new DescribeScheduledActionsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class Filters extends EucalyptusData {
  public Filters() {  }
  ArrayList<Filter> member = new ArrayList<Filter>();
}
public class ResumeProcessesType extends AutoScalingMessage {
  String autoScalingGroupName;
  ProcessNames scalingProcesses;
  public ResumeProcessesType() {  }
}
public class DescribeAdjustmentTypesResponseType extends AutoScalingMessage {
  public DescribeAdjustmentTypesResponseType() {  }
  DescribeAdjustmentTypesResult describeAdjustmentTypesResult = new DescribeAdjustmentTypesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class InstanceIds extends EucalyptusData {
  public InstanceIds() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class SuspendProcessesType extends AutoScalingMessage {
  String autoScalingGroupName;
  ProcessNames scalingProcesses;
  public SuspendProcessesType() {  }
}
public class LaunchConfigurations extends AutoScalingMessage {
  public LaunchConfigurations() {  }
  ArrayList<LaunchConfiguration> member = new ArrayList<LaunchConfiguration>();
}
public class Instances extends AutoScalingMessage {
  public Instances() {  }
  ArrayList<Instance> member = new ArrayList<Instance>();
}
public class TerminateInstanceInAutoScalingGroupResult extends EucalyptusData {
  Activity activity;
  public TerminateInstanceInAutoScalingGroupResult() {  }
}
public class DescribeScheduledActionsResult extends EucalyptusData {
  ScheduledUpdateGroupActions scheduledUpdateGroupActions;
  String nextToken;
  public DescribeScheduledActionsResult() {  }
}
public class DescribeAutoScalingInstancesType extends AutoScalingMessage {
  InstanceIds instanceIds;
  BigInteger maxRecords;
  String nextToken;
  public DescribeAutoScalingInstancesType() {  }
}
public class DeleteTagsType extends AutoScalingMessage {
  Tags tags;
  public DeleteTagsType() {  }
}
public class UpdateAutoScalingGroupResponseType extends AutoScalingMessage {
  public UpdateAutoScalingGroupResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class EnabledMetric extends EucalyptusData {
  String metric;
  String granularity;
  public EnabledMetric() {  }
}
public class DescribePoliciesResponseType extends AutoScalingMessage {
  public DescribePoliciesResponseType() {  }
  DescribePoliciesResult describePoliciesResult = new DescribePoliciesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class Tag extends EucalyptusData {
  String resourceId;
  String resourceType;
  String key;
  String value;
  Boolean propagateAtLaunch;
  public Tag() {  }
}
public class DescribeTagsResponseType extends AutoScalingMessage {
  public DescribeTagsResponseType() {  }
  DescribeTagsResult describeTagsResult = new DescribeTagsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ScheduledUpdateGroupActions extends EucalyptusData {
  public ScheduledUpdateGroupActions() {  }
  ArrayList<ScheduledUpdateGroupAction> member = new ArrayList<ScheduledUpdateGroupAction>();
}
public class DeletePolicyResponseType extends AutoScalingMessage {
  public DeletePolicyResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class Instance extends EucalyptusData {
  String instanceId;
  String availabilityZone;
  String lifecycleState;
  String healthStatus;
  String launchConfigurationName;
  public Instance() {  }
}
public class ExecutePolicyResponseType extends AutoScalingMessage {
  public ExecutePolicyResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ActivityIds extends EucalyptusData {
  public ActivityIds() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class MetricGranularityType extends EucalyptusData {
  String granularity;
  public MetricGranularityType() {  }
}
public class AdjustmentTypes extends AutoScalingMessage {
  public AdjustmentTypes() {  }
  ArrayList<AdjustmentType> member = new ArrayList<AdjustmentType>();
}
public class PutScalingPolicyResponseType extends AutoScalingMessage {
  public PutScalingPolicyResponseType() {  }
  PutScalingPolicyResult putScalingPolicyResult = new PutScalingPolicyResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class Tags extends AutoScalingMessage {
  public Tags() {  }
  ArrayList<Tag> member = new ArrayList<Tag>();
}
public class SetDesiredCapacityResponseType extends AutoScalingMessage {
  public SetDesiredCapacityResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeScalingActivitiesType extends AutoScalingMessage {
  ActivityIds activityIds;
  String autoScalingGroupName;
  BigInteger maxRecords;
  String nextToken;
  public DescribeScalingActivitiesType() {  }
}
public class LaunchConfiguration extends AutoScalingMessage {
  String launchConfigurationName;
  String launchConfigurationARN;
  String imageId;
  String keyName;
  SecurityGroups securityGroups;
  String userData;
  String instanceType;
  String kernelId;
  String ramdiskId;
  BlockDeviceMappings blockDeviceMappings;
  InstanceMonitoring instanceMonitoring;
  String spotPrice;
  String iamInstanceProfile;
  Date createdTime;
  public LaunchConfiguration() {  }
}
public class Processes extends AutoScalingMessage {
  public Processes() {  }
  ArrayList<ProcessType> member = new ArrayList<ProcessType>();
}
public class Ebs extends EucalyptusData {
  String snapshotId;
  BigInteger volumeSize;
  public Ebs() {  }
}
public class SetInstanceHealthType extends AutoScalingMessage {
  String instanceId;
  String healthStatus;
  Boolean shouldRespectGracePeriod;
  public SetInstanceHealthType() {  }
}
public class UpdateAutoScalingGroupType extends AutoScalingMessage {
  String autoScalingGroupName;
  String launchConfigurationName;
  BigInteger minSize;
  BigInteger maxSize;
  BigInteger desiredCapacity;
  BigInteger defaultCooldown;
  AvailabilityZones availabilityZones;
  String healthCheckType;
  BigInteger healthCheckGracePeriod;
  String placementGroup;
  String vpcZoneIdentifier;
  TerminationPolicies terminationPolicies;
  public UpdateAutoScalingGroupType() {  }
}
public class DescribeMetricCollectionTypesResult extends EucalyptusData {
  MetricCollectionTypes metrics;
  MetricGranularityTypes granularities;
  public DescribeMetricCollectionTypesResult() {  }
}
public class BlockDeviceMapping extends EucalyptusData {
  String virtualName;
  String deviceName;
  Ebs ebs;
  public BlockDeviceMapping() {  }
}
public class ScalingPolicies extends EucalyptusData {
  public ScalingPolicies() {  }
  ArrayList<ScalingPolicy> member = new ArrayList<ScalingPolicy>();
}
public class ResponseMetadata extends EucalyptusData {
  String requestId;
  public ResponseMetadata() {  }
}
public class DescribeTerminationPolicyTypesResponseType extends AutoScalingMessage {
  public DescribeTerminationPolicyTypesResponseType() {  }
  DescribeTerminationPolicyTypesResult describeTerminationPolicyTypesResult = new DescribeTerminationPolicyTypesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeLaunchConfigurationsResult extends EucalyptusData {
  LaunchConfigurations launchConfigurations = new LaunchConfigurations();
  String nextToken;
  public DescribeLaunchConfigurationsResult() {  }
}
public class DescribePoliciesResult extends EucalyptusData {
  ScalingPolicies scalingPolicies;
  String nextToken;
  public DescribePoliciesResult() {  }
}
public class AutoScalingGroups extends AutoScalingMessage {
  public AutoScalingGroups() {  }
  ArrayList<AutoScalingGroup> member = new ArrayList<AutoScalingGroup>();
}
public class EnabledMetrics extends EucalyptusData {
  public EnabledMetrics() {  }
  ArrayList<EnabledMetric> member = new ArrayList<EnabledMetric>();
}
public class SetDesiredCapacityType extends AutoScalingMessage {
  String autoScalingGroupName;
  BigInteger desiredCapacity;
  Boolean honorCooldown;
  public SetDesiredCapacityType() {  }
}
public class PutScalingPolicyType extends AutoScalingMessage {
  String autoScalingGroupName;
  String policyName;
  BigInteger scalingAdjustment;
  String adjustmentType;
  BigInteger cooldown;
  BigInteger minAdjustmentStep;
  public PutScalingPolicyType() {  }
}
public class DescribeAutoScalingGroupsResponseType extends AutoScalingMessage {
  public DescribeAutoScalingGroupsResponseType() {  }
  DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class AutoScalingGroup extends AutoScalingMessage {
  String autoScalingGroupName;
  String autoScalingGroupARN;
  String launchConfigurationName;
  BigInteger minSize;
  BigInteger maxSize;
  BigInteger desiredCapacity;
  BigInteger defaultCooldown;
  AvailabilityZones availabilityZones;
  LoadBalancerNames loadBalancerNames;
  String healthCheckType;
  BigInteger healthCheckGracePeriod;
  Instances instances;
  Date createdTime;
  SuspendedProcesses suspendedProcesses;
  String placementGroup;
  String vpcZoneIdentifier;
  EnabledMetrics enabledMetrics;
  String status;
  TagDescriptionList tags;
  TerminationPolicies terminationPolicies;
  public AutoScalingGroup() {  }
}
public class DeleteLaunchConfigurationType extends AutoScalingMessage {
  String launchConfigurationName;
  public DeleteLaunchConfigurationType() {  }
}
public class DeleteAutoScalingGroupResponseType extends AutoScalingMessage {
  public DeleteAutoScalingGroupResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeScalingActivitiesResponseType extends AutoScalingMessage {
  public DescribeScalingActivitiesResponseType() {  }
  DescribeScalingActivitiesResult describeScalingActivitiesResult = new DescribeScalingActivitiesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ScalingPolicy extends AutoScalingMessage {
  String autoScalingGroupName;
  String policyName;
  BigInteger scalingAdjustment;
  String adjustmentType;
  BigInteger cooldown;
  String policyARN;
  Alarms alarms;
  BigInteger minAdjustmentStep;
  public ScalingPolicy() {  }
}
public class AutoScalingGroupNames extends EucalyptusData {
  public AutoScalingGroupNames() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class Values extends EucalyptusData {
  public Values() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class Error extends EucalyptusData {
  String type;
  String code;
  String message;
  public Error() {  }
  ErrorDetail detail = new ErrorDetail();
}
public class CreateOrUpdateTagsResponseType extends AutoScalingMessage {
  public CreateOrUpdateTagsResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class Activities extends AutoScalingMessage {
  public Activities() {  }
  ArrayList<Activity> member = new ArrayList<Activity>();
}
public class DescribePoliciesType extends AutoScalingMessage {
  String autoScalingGroupName;
  PolicyNames policyNames;
  String nextToken;
  BigInteger maxRecords;
  public DescribePoliciesType() {  }
}
public class PutScalingPolicyResult extends EucalyptusData {
  String policyARN;
  public PutScalingPolicyResult() {  }
}
public class ResumeProcessesResponseType extends AutoScalingMessage {
  public ResumeProcessesResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeAutoScalingGroupsType extends AutoScalingMessage {
  AutoScalingGroupNames autoScalingGroupNames;
  String nextToken;
  BigInteger maxRecords;
  public DescribeAutoScalingGroupsType() {  }
}
public class CreateLaunchConfigurationType extends AutoScalingMessage {
  String launchConfigurationName;
  String imageId;
  String keyName;
  SecurityGroups securityGroups;
  String userData;
  String instanceType;
  String kernelId;
  String ramdiskId;
  BlockDeviceMappings blockDeviceMappings;
  InstanceMonitoring instanceMonitoring;
  String spotPrice;
  String iamInstanceProfile;
  public CreateLaunchConfigurationType() {  }
}
public class MetricCollectionType extends EucalyptusData {
  String metric;
  public MetricCollectionType() {  }
}
public class DescribeLaunchConfigurationsResponseType extends AutoScalingMessage {
  public DescribeLaunchConfigurationsResponseType() {  }
  DescribeLaunchConfigurationsResult describeLaunchConfigurationsResult = new DescribeLaunchConfigurationsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeScalingProcessTypesType extends AutoScalingMessage {
  public DescribeScalingProcessTypesType() {  }
}
public class CreateOrUpdateTagsType extends AutoScalingMessage {
  Tags tags;
  public CreateOrUpdateTagsType() {  }
}
public class ProcessNames extends EucalyptusData {
  public ProcessNames() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class DescribeAdjustmentTypesResult extends EucalyptusData {
  AdjustmentTypes adjustmentTypes;
  public DescribeAdjustmentTypesResult() {  }
}
public class DescribeScalingProcessTypesResult extends EucalyptusData {
  Processes processes;
  public DescribeScalingProcessTypesResult() {  }
}
public class DescribeAutoScalingNotificationTypesResult extends EucalyptusData {
  AutoScalingNotificationTypes autoScalingNotificationTypes;
  public DescribeAutoScalingNotificationTypesResult() {  }
}
