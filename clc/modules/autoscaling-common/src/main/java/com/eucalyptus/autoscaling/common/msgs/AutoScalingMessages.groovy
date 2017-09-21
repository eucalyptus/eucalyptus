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
@GroovyAddClassUUID
package com.eucalyptus.autoscaling.common.msgs

import com.eucalyptus.autoscaling.common.AutoScaling
import com.eucalyptus.autoscaling.common.AutoScalingMessageValidation
import com.eucalyptus.ws.WebServiceError
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import com.eucalyptus.component.annotation.ComponentMessage
import com.google.common.collect.Lists
import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping
import java.lang.reflect.Field //TODO:STEVE: rmeove
import javax.annotation.Nonnull
import com.google.common.collect.Maps
import com.google.common.base.Function
import com.eucalyptus.util.CollectionUtils
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID
import com.google.common.base.Predicate

import java.lang.reflect.Method

import static com.eucalyptus.util.MessageValidation.validateRecursively

class DescribeMetricCollectionTypesType extends AutoScalingMessage {
}
class Alarm extends EucalyptusData {
  String alarmName
  String alarmARN
}
class MetricGranularityTypes extends EucalyptusData {
  MetricGranularityTypes() {  }
  MetricGranularityTypes( final Collection<MetricGranularityType> member ) {
    this.member = Lists.newArrayList( member )
  }
  ArrayList<MetricGranularityType> member = Lists.newArrayList( )
}
class DescribeAutoScalingNotificationTypesResponseType extends AutoScalingMessage {
  DescribeAutoScalingNotificationTypesResult describeAutoScalingNotificationTypesResult = new DescribeAutoScalingNotificationTypesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class LaunchConfigurationNames extends EucalyptusData {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
@ComponentMessage(AutoScaling)
class AutoScalingMessage extends BaseMessage {

  @Override
  def <TYPE extends BaseMessage> TYPE getReply() {
    TYPE type = super.getReply()
    getResponseMetadata( type )?.with{
      requestId = getCorrelationId( )
    }
    return type
  }

  static ResponseMetadata getResponseMetadata( final BaseMessage message ) {
    try {
      Method responseMetadataMethod = message.class.getMethod("getResponseMetadata")
      return ((ResponseMetadata) responseMetadataMethod.invoke( message ))
    } catch ( Exception e ) {
    }
    null
  }

  Map<String,String> validate( ) {
    validateRecursively(
        Maps.<String,String>newTreeMap( ),
        new AutoScalingMessageValidation.AutoScalingMessageValidationAssistant(),
        "",
        this )
  }
}
class SuspendProcessesResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class AutoScalingNotificationTypes extends EucalyptusData {
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
class TerminateInstanceInAutoScalingGroupType extends AutoScalingMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_INSTANCE)
  String instanceId
  @Nonnull
  Boolean shouldDecrementDesiredCapacity
}
class ErrorResponse extends AutoScalingMessage implements WebServiceError {
  String requestId
  ArrayList<Error> error = new ArrayList<Error>( )

  ErrorResponse( ) {
    set_return( false )
  }

  @Override
  String toSimpleString( ) {
    "${error?.getAt(0)?.type} error (${webServiceErrorCode}): ${webServiceErrorMessage}"
  }

  @Override
  String getWebServiceErrorCode( ) {
    error?.getAt(0)?.code
  }

  @Override
  String getWebServiceErrorMessage( ) {
    error?.getAt(0)?.message
  }
}
class BlockDeviceMappings extends EucalyptusData {
  BlockDeviceMappings() {  }
  BlockDeviceMappings( Collection<BlockDeviceMappingType> mappings ) {
    member.addAll( mappings )
  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<BlockDeviceMappingType> member = new ArrayList<BlockDeviceMappingType>()
}
class LoadBalancerNames extends EucalyptusData {
  LoadBalancerNames() {  }
  LoadBalancerNames( Collection<String> names ) {
    if ( names != null ) member.addAll( names )
  }
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.ELB_NAME)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
class PolicyNames extends EucalyptusData {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
class DescribeTerminationPolicyTypesType extends AutoScalingMessage {
}
class DeleteTagsResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class SetInstanceHealthResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class DeleteAutoScalingGroupType extends AutoScalingMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  Boolean forceDelete
}
class DescribeNotificationConfigurationsType extends AutoScalingMessage {
  AutoScalingGroupNames autoScalingGroupNames
  String nextToken
  @AutoScalingMessageValidation.FieldRange(min=1L,max=100L)
  Integer maxRecords
}
class ScheduledUpdateGroupAction extends EucalyptusData {
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
}
class PutScheduledUpdateGroupActionResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class ProcessType extends EucalyptusData {
  String processName
  ProcessType() {  }
  ProcessType( String processName ) {
    this.processName = processName
  }
}
class TagDescription extends EucalyptusData {
  String resourceId
  String resourceType
  String key
  String value
  Boolean propagateAtLaunch
}
class DeleteNotificationConfigurationType extends AutoScalingMessage {
  String autoScalingGroupName
  String topicARN
}
class ExecutePolicyType extends AutoScalingMessage {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String policyName
  Boolean honorCooldown
}
class AutoScalingInstanceDetails extends EucalyptusData {
  String instanceId
  String autoScalingGroupName
  String availabilityZone
  String lifecycleState
  String healthStatus
  String launchConfigurationName
  Boolean protectedFromScaleIn
}
class DeletePolicyType extends AutoScalingMessage {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String policyName
}
class DescribeAutoScalingGroupsResult extends EucalyptusData {
  AutoScalingGroupsType autoScalingGroups = new AutoScalingGroupsType()
  String nextToken
}
class CreateLaunchConfigurationResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class TerminateInstanceInAutoScalingGroupResponseType extends AutoScalingMessage {
  TerminateInstanceInAutoScalingGroupResult terminateInstanceInAutoScalingGroupResult = new TerminateInstanceInAutoScalingGroupResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class DescribeAutoScalingInstancesResponseType extends AutoScalingMessage {
  DescribeAutoScalingInstancesResult describeAutoScalingInstancesResult = new DescribeAutoScalingInstancesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class PutNotificationConfigurationType extends AutoScalingMessage {
  String autoScalingGroupName
  String topicARN
  AutoScalingNotificationTypes notificationTypes
}
class MetricCollectionTypes extends EucalyptusData {
  MetricCollectionTypes() {  }
  MetricCollectionTypes( Collection<String> types ) {
    member.addAll( types.collect{ String type -> new MetricCollectionType( metric: type ) } )
  }
  ArrayList<MetricCollectionType> member = new ArrayList<MetricCollectionType>()
}
class CreateAutoScalingGroupResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class DeleteNotificationConfigurationResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class TagDescriptionList extends EucalyptusData {
  ArrayList<TagDescription> member = new ArrayList<TagDescription>()
}
class DescribeTerminationPolicyTypesResult extends EucalyptusData {
  TerminationPolicies terminationPolicyTypes = new TerminationPolicies()
}
class DescribeScalingProcessTypesResponseType extends AutoScalingMessage {
  DescribeScalingProcessTypesResult describeScalingProcessTypesResult = new DescribeScalingProcessTypesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class PutNotificationConfigurationResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class Activity extends EucalyptusData {
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
  Activity() {  }

  static Function<Activity, String> startTime() {
    { Activity activity -> activity.startTime } as Function<Activity, String>
  }
}
class SuspendedProcesses extends EucalyptusData {
  ArrayList<SuspendedProcessType> member = new ArrayList<SuspendedProcessType>()
}
class InstanceMonitoring extends EucalyptusData {
  Boolean enabled
  InstanceMonitoring() {  }
  InstanceMonitoring( Boolean enabled ) {
    this.enabled = enabled
  }
}
class DescribeScheduledActionsType extends AutoScalingMessage {
  String autoScalingGroupName
  ScheduledActionNames scheduledActionNames
  Date startTime
  Date endTime
  String nextToken
  @AutoScalingMessageValidation.FieldRange(min=1L,max=100L)
  Integer maxRecords
}
class Filter extends EucalyptusData {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.TAG_FILTER)
  String name
  Values values
  List<String> values() {
    values != null ?
      values.getMember() :
      []
  }
}
class ErrorDetail extends EucalyptusData {
}
class Alarms extends EucalyptusData {
  Alarms() {  }
  Alarms( Iterable<String> alarmArns ) {
    alarmArns.each { String alarmArn ->
      int nameIndex = alarmArn.indexOf( ":alarm:" );
      String alarmName = nameIndex > 0 ?
        alarmArn.substring( nameIndex + 7 ) :
        null
      member.add( new Alarm(
          alarmARN: alarmArn,
          alarmName: alarmName
      ) );
    }
  }
  ArrayList<Alarm> member = new ArrayList<Alarm>()
}
class DescribeAutoScalingInstancesResult extends EucalyptusData {
  AutoScalingInstances autoScalingInstances = new AutoScalingInstances()
  String nextToken
}
class DescribeLaunchConfigurationsType extends AutoScalingMessage {
  LaunchConfigurationNames launchConfigurationNames = new LaunchConfigurationNames()
  String nextToken
  @AutoScalingMessageValidation.FieldRange(min=1L,max=100L)
  Integer maxRecords
  List<String> launchConfigurationNames() {
    List<String> names = Lists.newArrayList()
    if ( launchConfigurationNames != null ) {
      names = launchConfigurationNames.getMember()
    }
    return names
  }
}
class DescribeMetricCollectionTypesResponseType extends AutoScalingMessage {
  DescribeMetricCollectionTypesResult describeMetricCollectionTypesResult = new DescribeMetricCollectionTypesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class AutoScalingInstances extends EucalyptusData {
  ArrayList<AutoScalingInstanceDetails> member = new ArrayList<AutoScalingInstanceDetails>()
}
class DescribeTagsType extends AutoScalingMessage {
  Filters filters
  String nextToken
  @AutoScalingMessageValidation.FieldRange(min=1L,max=100L)
  Integer maxRecords
  List<Filter> filters() {
    filters != null ?
        filters.member :
        []
  }
}
class AdjustmentType extends EucalyptusData {
  String adjustmentType
}
class DeleteScheduledActionType extends AutoScalingMessage {
  String autoScalingGroupName
  String scheduledActionName
}
class DisableMetricsCollectionResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class CreateAutoScalingGroupType extends AutoScalingMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME)
  String autoScalingGroupName
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String launchConfigurationName
  @Nonnull @AutoScalingMessageValidation.FieldRange
  Integer minSize
  @Nonnull @AutoScalingMessageValidation.FieldRange
  Integer maxSize
  @AutoScalingMessageValidation.FieldRange
  Integer desiredCapacity
  @AutoScalingMessageValidation.FieldRange
  Integer defaultCooldown
  AvailabilityZones availabilityZones
  LoadBalancerNames loadBalancerNames
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.HEALTH_CHECK)
  String healthCheckType
  @AutoScalingMessageValidation.FieldRange
  Integer healthCheckGracePeriod
  Boolean newInstancesProtectedFromScaleIn
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  String placementGroup
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.VPC_ZONE_IDENTIFIER)
  @HttpParameterMapping(parameter="VPCZoneIdentifier")
  String vpcZoneIdentifier
  TerminationPolicies terminationPolicies
  Tags tags
  Collection<String> availabilityZones() {
    return availabilityZones?.member
  }
  Collection<String> loadBalancerNames() {
    return loadBalancerNames?.member
  }
  Collection<String> terminationPolicies() {
    return terminationPolicies?.member
  }

  @Override
  Map<String, String> validate() {
    Map<String,String> errors = super.validate()
    if ( minSize && maxSize && minSize > maxSize ) {
      errors.put( "MinSize", "MinSize must not be greater than MaxSize" )
    }
    if ( minSize && desiredCapacity && desiredCapacity < minSize ) {
      errors.put( "DesiredCapacity", "DesiredCapacity must not be less than MinSize" )
    }
    if ( maxSize && desiredCapacity && desiredCapacity > maxSize ) {
      errors.put( "DesiredCapacity", "DesiredCapacity must not be greater than MaxSize" )
    }
    if ( availabilityZones && availabilityZones.member.isEmpty() ) {
      errors.put( "AvailabilityZones.member.1", "AvailabilityZones.member.1 is required" )
    }
    if ( vpcZoneIdentifier == null && (!availabilityZones || availabilityZones.member.isEmpty( ) ) ) {
      errors.put( "AvailabilityZones.member.1", "One of AvailabilityZones or VPCZoneIdentifier is required" )
    }
    errors
  }
}
class DisableMetricsCollectionType extends AutoScalingMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  Metrics metrics
}
class DescribeAdjustmentTypesType extends AutoScalingMessage {
}
class TerminationPolicies extends EucalyptusData {
  TerminationPolicies() {  }
  TerminationPolicies( Collection<String> terminationPolicies ) {
    if ( terminationPolicies != null ) member.addAll( terminationPolicies )
  }
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.TERMINATION_POLICY)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
class NotificationConfiguration extends EucalyptusData {
  String autoScalingGroupName
  String topicARN
  String notificationType
}
class DescribeTagsResult extends EucalyptusData {
  TagDescriptionList tags
  String nextToken
}
class DescribeNotificationConfigurationsResponseType extends AutoScalingMessage {
  DescribeNotificationConfigurationsResult describeNotificationConfigurationsResult = new DescribeNotificationConfigurationsResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class ScheduledActionNames extends EucalyptusData {
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
class AvailabilityZones extends EucalyptusData {
  AvailabilityZones() {  }
  AvailabilityZones( Collection<String> zones ) {
    if ( zones != null ) member.addAll( zones )
  }
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
class DescribeScalingActivitiesResult extends EucalyptusData {
  Activities activities = new Activities()
  String nextToken
}
class DescribeAutoScalingNotificationTypesType extends AutoScalingMessage {
}
class Metrics extends EucalyptusData {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.METRIC)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
class DeleteScheduledActionResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class DescribeNotificationConfigurationsResult extends EucalyptusData {
  NotificationConfigurations notificationConfigurations = new NotificationConfigurations()
  String nextToken
}
class EnableMetricsCollectionType extends AutoScalingMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  Metrics metrics
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.METRIC_GRANULARITY)
  String granularity
}
class EnableMetricsCollectionResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class PutScheduledUpdateGroupActionType extends AutoScalingMessage {
  String autoScalingGroupName
  String scheduledActionName
  Date time
  Date startTime
  Date endTime
  String recurrence
  Integer minSize
  Integer maxSize
  Integer desiredCapacity
}
class SuspendedProcessType extends EucalyptusData {
  String processName
  String suspensionReason
}
class SecurityGroups extends EucalyptusData {
  SecurityGroups() {  }
  SecurityGroups( Collection<String> groups ) {
    member.addAll( groups )
  }
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
class NotificationConfigurations extends EucalyptusData {
  ArrayList<NotificationConfiguration> member = new ArrayList<NotificationConfiguration>()
}
class DeleteLaunchConfigurationResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class DescribeScheduledActionsResponseType extends AutoScalingMessage {
  DescribeScheduledActionsResult describeScheduledActionsResult = new DescribeScheduledActionsResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class Filters extends EucalyptusData {
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<Filter> member = new ArrayList<Filter>()
}
class ResumeProcessesType extends AutoScalingMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  ProcessNames scalingProcesses
}
class DescribeAdjustmentTypesResponseType extends AutoScalingMessage {
  DescribeAdjustmentTypesResult describeAdjustmentTypesResult = new DescribeAdjustmentTypesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class InstanceIds extends EucalyptusData {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_INSTANCE_VERBOSE)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
class SuspendProcessesType extends AutoScalingMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  ProcessNames scalingProcesses
}
class LaunchConfigurationsType extends EucalyptusData {
  ArrayList<LaunchConfigurationType> member = new ArrayList<LaunchConfigurationType>()
}
class Instances extends EucalyptusData {
  ArrayList<Instance> member = new ArrayList<Instance>()
}
class TerminateInstanceInAutoScalingGroupResult extends EucalyptusData {
  Activity activity
}
class DescribeScheduledActionsResult extends EucalyptusData {
  ScheduledUpdateGroupActions scheduledUpdateGroupActions
  String nextToken
}
class DescribeAutoScalingInstancesType extends AutoScalingMessage {
  InstanceIds instanceIds
  @AutoScalingMessageValidation.FieldRange(min=1L,max=100L)
  Integer maxRecords
  String nextToken
  List<String> instanceIds() {
    List<String> names = Lists.newArrayList()
    if ( instanceIds != null ) {
      names = instanceIds.getMember()
    }
    return names
  }
}
class DeleteTagsType extends AutoScalingMessage {
  @Nonnull
  Tags tags
}
class UpdateAutoScalingGroupResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class EnabledMetric extends EucalyptusData {
  String metric
  String granularity = "1Minute"
}
class DescribePoliciesResponseType extends AutoScalingMessage {
  DescribePoliciesResult describePoliciesResult = new DescribePoliciesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class TagType extends EucalyptusData {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME)
  String resourceId
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.TAG_RESOURCE)
  String resourceType
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.STRING_128)
  String key
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.STRING_256)
  String value
  Boolean propagateAtLaunch
}
class DescribeTagsResponseType extends AutoScalingMessage {
  DescribeTagsResult describeTagsResult = new DescribeTagsResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class ScheduledUpdateGroupActions extends EucalyptusData {
  ArrayList<ScheduledUpdateGroupAction> member = new ArrayList<ScheduledUpdateGroupAction>()
}
class DeletePolicyResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class Instance extends EucalyptusData {
  String instanceId
  String availabilityZone
  String lifecycleState
  String healthStatus
  String launchConfigurationName
  Boolean protectedFromScaleIn
}
class ExecutePolicyResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class ActivityIds extends EucalyptusData {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.UUID_VERBOSE)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
class MetricGranularityType extends EucalyptusData {
  String granularity
  MetricGranularityType() {  }
  MetricGranularityType(final String granularity) {
    this.granularity = granularity
  }
}
class AdjustmentTypes extends EucalyptusData {
  AdjustmentTypes() {  }
  AdjustmentTypes( Collection<String> values ) {
    member = values.collect { String value ->
      new AdjustmentType( adjustmentType: value )
    } as ArrayList<AdjustmentType>
  }
  ArrayList<AdjustmentType> member = new ArrayList<AdjustmentType>()
}
class PutScalingPolicyResponseType extends AutoScalingMessage {
  PutScalingPolicyResult putScalingPolicyResult = new PutScalingPolicyResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class Tags extends EucalyptusData {
  @HttpParameterMapping(parameter="member")
  @HttpEmbedded(multiple=true)
  ArrayList<TagType> member = new ArrayList<TagType>()
}
class SetDesiredCapacityResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class DescribeScalingActivitiesType extends AutoScalingMessage {
  ActivityIds activityIds
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  @AutoScalingMessageValidation.FieldRange(min=1L,max=100L)
  Integer maxRecords
  String nextToken
  List<String> activityIds() {
    List<String> ids = Lists.newArrayList()
    if ( activityIds != null ) {
      ids = activityIds.getMember()
    }
    return ids
  }
}
class LaunchConfigurationType extends EucalyptusData {
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
  Boolean associatePublicIpAddress
}
class Processes extends EucalyptusData {
  ArrayList<ProcessType> member = new ArrayList<ProcessType>()
}
class Ebs extends EucalyptusData {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_SNAPSHOT)
  String snapshotId
  @AutoScalingMessageValidation.FieldRange
  Integer volumeSize
}
class SetInstanceHealthType extends AutoScalingMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_INSTANCE)
  String instanceId
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.HEALTH_STATUS)
  String healthStatus
  Boolean shouldRespectGracePeriod
}
class UpdateAutoScalingGroupType extends AutoScalingMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String launchConfigurationName
  @AutoScalingMessageValidation.FieldRange
  Integer minSize
  @AutoScalingMessageValidation.FieldRange
  Integer maxSize
  @AutoScalingMessageValidation.FieldRange
  Integer desiredCapacity
  @AutoScalingMessageValidation.FieldRange
  Integer defaultCooldown
  AvailabilityZones availabilityZones
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.HEALTH_CHECK)
  String healthCheckType
  @AutoScalingMessageValidation.FieldRange
  Integer healthCheckGracePeriod
  Boolean newInstancesProtectedFromScaleIn
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  String placementGroup
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  @HttpParameterMapping(parameter="VPCZoneIdentifier")
  String vpcZoneIdentifier
  TerminationPolicies terminationPolicies
  Collection<String> availabilityZones() {
    return availabilityZones?.member
  }
  Collection<String> terminationPolicies() {
    return terminationPolicies?.member
  }
  @Override
  Map<String, String> validate() {
    Map<String,String> errors = super.validate()
    if ( minSize && maxSize && minSize > maxSize ) {
      errors.put( "MinSize", "MinSize must not be greater than MaxSize" )
    }
    if ( minSize && desiredCapacity && desiredCapacity < minSize ) {
      errors.put( "DesiredCapacity", "DesiredCapacity must not be less than MinSize" )
    }
    if ( maxSize && desiredCapacity && desiredCapacity > maxSize ) {
      errors.put( "DesiredCapacity", "DesiredCapacity must not be greater than MaxSize" )
    }
    errors
  }
}
class DescribeMetricCollectionTypesResult extends EucalyptusData {
  MetricCollectionTypes metrics
  MetricGranularityTypes granularities = new MetricGranularityTypes()
}
class BlockDeviceMappingType extends EucalyptusData {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  String virtualName
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  String deviceName
  Ebs ebs
  BlockDeviceMappingType() {  }
  BlockDeviceMappingType( String deviceName, String virtualName, String snapshotId, Integer volumeSize ) {
    this.deviceName = deviceName
    this.virtualName = virtualName
    if ( snapshotId != null || volumeSize != null ) {
      this.ebs = new Ebs( snapshotId: snapshotId, volumeSize: volumeSize )
    }
  }
}
class ScalingPolicies extends EucalyptusData {
  ArrayList<ScalingPolicyType> member = new ArrayList<ScalingPolicyType>()
}
class ResponseMetadata extends EucalyptusData {
  String requestId
}
class DescribeTerminationPolicyTypesResponseType extends AutoScalingMessage {
  DescribeTerminationPolicyTypesResult describeTerminationPolicyTypesResult = new DescribeTerminationPolicyTypesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class DescribeLaunchConfigurationsResult extends EucalyptusData {
  LaunchConfigurationsType launchConfigurations = new LaunchConfigurationsType()
  String nextToken
}
class DescribePoliciesResult extends EucalyptusData {
  ScalingPolicies scalingPolicies = new ScalingPolicies()
  String nextToken
}
class AutoScalingGroupsType extends EucalyptusData {
  ArrayList<AutoScalingGroupType> member = new ArrayList<AutoScalingGroupType>()
}
class EnabledMetrics extends EucalyptusData {
  EnabledMetrics() {  }
  EnabledMetrics( Collection<String> enabledMetrics ) {
    if ( enabledMetrics != null ) member.addAll( enabledMetrics.collect{ String metric -> new EnabledMetric(metric: metric) } )
  }
  ArrayList<EnabledMetric> member = new ArrayList<EnabledMetric>()
}
class SetDesiredCapacityType extends AutoScalingMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  @Nonnull @AutoScalingMessageValidation.FieldRange
  Integer desiredCapacity
  Boolean honorCooldown
}
class PutScalingPolicyType extends AutoScalingMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME)
  String policyName
  @Nonnull
  Integer scalingAdjustment
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.ADJUSTMENT)
  String adjustmentType
  @AutoScalingMessageValidation.FieldRange
  Integer cooldown
  Integer minAdjustmentStep
}
class DescribeAutoScalingGroupsResponseType extends AutoScalingMessage {
  DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class AutoScalingGroupType extends EucalyptusData {
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
  Boolean newInstancesProtectedFromScaleIn
  String placementGroup
  String vpcZoneIdentifier
  EnabledMetrics enabledMetrics
  String status
  TagDescriptionList tags
  TerminationPolicies terminationPolicies

  static Function<AutoScalingGroupType, String> groupName() {
    { AutoScalingGroupType group -> group.autoScalingGroupName } as Function<AutoScalingGroupType, String>
  }
}
class DeleteLaunchConfigurationType extends AutoScalingMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String launchConfigurationName
}
class DeleteAutoScalingGroupResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class DescribeScalingActivitiesResponseType extends AutoScalingMessage {
  DescribeScalingActivitiesResult describeScalingActivitiesResult = new DescribeScalingActivitiesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class ScalingPolicyType extends EucalyptusData {
  String autoScalingGroupName
  String policyName
  Integer scalingAdjustment
  String adjustmentType
  Integer cooldown
  String policyARN
  Alarms alarms
  Integer minAdjustmentStep

  static Function<ScalingPolicyType, String> policyArn() {
    { ScalingPolicyType policy -> policy.policyARN } as Function<ScalingPolicyType, String>
  }
}
class AutoScalingGroupNames extends EucalyptusData {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
class Values extends EucalyptusData {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.STRING_256)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
class Error extends EucalyptusData {
  String type
  String code
  String message
  ErrorDetail detail = new ErrorDetail()
}
class CreateOrUpdateTagsResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class Activities extends EucalyptusData {
  ArrayList<Activity> member = new ArrayList<Activity>()
}
class DescribePoliciesType extends AutoScalingMessage {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  PolicyNames policyNames
  String nextToken
  @AutoScalingMessageValidation.FieldRange(min=1L,max=100L)
  Integer maxRecords
  List<String> policyNames() {
    List<String> names = Lists.newArrayList()
    if ( policyNames != null ) {
      names = policyNames.getMember()
    }
    return names
  }
}
class PutScalingPolicyResult extends EucalyptusData {
  String policyARN
}
class ResumeProcessesResponseType extends AutoScalingMessage {
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class DescribeAutoScalingGroupsType extends AutoScalingMessage {
  AutoScalingGroupNames autoScalingGroupNames
  String nextToken
  @AutoScalingMessageValidation.FieldRange(min=1L,max=100L)
  Integer maxRecords
  List<String> autoScalingGroupNames() {
    List<String> names = Lists.newArrayList()
    if ( autoScalingGroupNames != null ) {
      names = autoScalingGroupNames.getMember()
    }
    return names
  }
}
class CreateLaunchConfigurationType extends AutoScalingMessage {
  Boolean associatePublicIpAddress
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME)
  String launchConfigurationName
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_MACHINE_IMAGE)
  String imageId
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  String keyName
  SecurityGroups securityGroups
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_USERDATA)
  String userData
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  String instanceType
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_KERNEL_IMAGE)
  String kernelId
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_RAMDISK_IMAGE)
  String ramdiskId
  BlockDeviceMappings blockDeviceMappings
  InstanceMonitoring instanceMonitoring
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_PLACEMENT_TENANCY)
  String placementTenancy
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_SPOT_PRICE)
  String spotPrice
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.IAM_NAME_OR_ARN)
  String iamInstanceProfile
  Boolean ebsOptimized

  @Override
  Map<String, String> validate() {
    Map<String,String> errors = super.validate()
    // Validate security group identifiers or names used consistently
    if ( securityGroups != null && securityGroups.member != null ) {
      int idCount = CollectionUtils.reduce(
          securityGroups.member,
          0,
          CollectionUtils.count( { String group -> group.matches( "sg-[0-9A-Fa-f]{8}" ) } as Predicate<String> ) )
      if ( idCount != 0 && idCount != securityGroups.member.size() ) {
        errors.put(
            "SecurityGroups.member",
            "Must use either use group-id or group-name for all the security groups, not both at the same time"  )
      }
    }
    errors
  }
}
class MetricCollectionType extends EucalyptusData {
  String metric
}
class DescribeLaunchConfigurationsResponseType extends AutoScalingMessage {
  DescribeLaunchConfigurationsResult describeLaunchConfigurationsResult = new DescribeLaunchConfigurationsResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class DescribeScalingProcessTypesType extends AutoScalingMessage {
}
class CreateOrUpdateTagsType extends AutoScalingMessage {
  @Nonnull
  Tags tags
}
class ProcessNames extends EucalyptusData {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.SCALING_PROCESS)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
class DescribeAdjustmentTypesResult extends EucalyptusData {
  AdjustmentTypes adjustmentTypes
}
class DescribeScalingProcessTypesResult extends EucalyptusData {
  Processes processes = new Processes()
}
class DescribeAutoScalingNotificationTypesResult extends EucalyptusData {
  AutoScalingNotificationTypes autoScalingNotificationTypes
}
class DescribeAccountLimitsType extends AutoScalingMessage {
}
class DescribeAccountLimitsResponseType extends AutoScalingMessage {
  DescribeAccountLimitsResult describeAccountLimitsResult = new DescribeAccountLimitsResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class DescribeAccountLimitsResult extends EucalyptusData {
}
class SetInstanceProtectionType extends AutoScalingMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  @Nonnull
  InstanceIds instanceIds
  @Nonnull
  Boolean protectedFromScaleIn
}
class SetInstanceProtectionResponseType extends AutoScalingMessage {
  SetInstanceProtectionResult setInstanceProtectionResult = new SetInstanceProtectionResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
class SetInstanceProtectionResult extends EucalyptusData {
}
