/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
@GroovyAddClassUUID
package com.eucalyptus.autoscaling.common.backend.msgs

import com.eucalyptus.autoscaling.common.AutoScalingBackend
import com.eucalyptus.autoscaling.common.AutoScalingMessageValidation
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import com.eucalyptus.component.annotation.ComponentMessage
import com.google.common.collect.Lists
import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping
import java.lang.reflect.Field
import javax.annotation.Nonnull
import com.eucalyptus.system.Ats
import com.google.common.collect.Maps
import com.google.common.base.Function
import com.eucalyptus.util.CollectionUtils
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID
import com.google.common.base.Predicate

public class DescribeMetricCollectionTypesType extends AutoScalingBackendMessage {
  public DescribeMetricCollectionTypesType() {  }
}
public class Alarm extends EucalyptusData {
  String alarmName
  String alarmARN
  public Alarm() {  }
}
public class MetricGranularityTypes extends EucalyptusData {
  public MetricGranularityTypes() {  }
  ArrayList<MetricGranularityType> member = [ new MetricGranularityType(granularity: "1Minute") ] as ArrayList<MetricGranularityType>
}
public class DescribeAutoScalingNotificationTypesResponseType extends AutoScalingBackendMessage {
  public DescribeAutoScalingNotificationTypesResponseType() {  }
  DescribeAutoScalingNotificationTypesResult describeAutoScalingNotificationTypesResult = new DescribeAutoScalingNotificationTypesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class LaunchConfigurationNames extends EucalyptusData {
  public LaunchConfigurationNames() {  }
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
@ComponentMessage(AutoScalingBackend.class)
public class AutoScalingBackendMessage extends BaseMessage {

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

  Map<String,String> validate( ) {
    Map<String,String> errors = Maps.newTreeMap()
    validateRecursively( errors, "", this )
    errors
  }

  static void validateRecursively( Map<String,String> errorMap,
                                   String prefix,
                                   Object target ) {
    target.class.declaredFields.each { Field field ->
      Ats fieldAts = Ats.from( field )
      field.setAccessible( true )
      Object value = field.get( target );
      String displayName = prefix + AutoScalingMessageValidation.displayName( field )

      // validate null constraint
      if ( fieldAts.has( Nonnull.class ) && value == null ) {
        errorMap.put( displayName, displayName + " is required" )
      }

      // validate regex
      AutoScalingMessageValidation.FieldRegex regex = fieldAts.get( AutoScalingMessageValidation.FieldRegex.class )
      if ( regex && value != null && !(value instanceof Iterable) && !regex.value().pattern().matcher( String.valueOf( value ) ).matches( ) ) {
        errorMap.put( displayName, String.valueOf(value) + " for parameter " + displayName + " is invalid" )
      } else if ( regex && value instanceof Iterable ) {
        value.eachWithIndex { Object item, int index  ->
          if ( !regex.value().pattern().matcher( String.valueOf( item ) ).matches( ) ) {
            errorMap.put( displayName + "." + (index + 1), String.valueOf(item) + " for parameter " + displayName + "." + (index + 1) + " is invalid" )
          }
        }
      }

      // validate range
      AutoScalingMessageValidation.FieldRange range = fieldAts.get( AutoScalingMessageValidation.FieldRange.class )
      if ( range != null && value instanceof Number ) {
        Long longValue = ((Number) value).longValue()
        if ( longValue < range.min() || longValue > range.max() ) {
          errorMap.put( displayName, String.valueOf(value) + " for parameter " + displayName + " is invalid" )
        }
      }
      if ( range != null && value instanceof List ) {
        Long longValue = (long) ((List)value).size()
        if ( longValue < range.min() && range.min() == 1 ) {
          errorMap.put( displayName + ".1", displayName + ".1 is required" )
        } else if ( longValue < range.min() ) {
          errorMap.put( displayName, displayName + " length too short" )
        } else if ( longValue > range.max() ) {
          errorMap.put( displayName, displayName + " length too long" )
        }
      }

      // validate recursively
      if ( value instanceof EucalyptusData ) {
        validateRecursively( errorMap, displayName + ".", value )
      } else if ( value instanceof Iterable ) {
        value.eachWithIndex { Object item, int index ->
          if ( item instanceof EucalyptusData ) {
            validateRecursively( errorMap, displayName + "." + (index + 1) + ".", item )
          }
        }
      }
    }
  }
}
public class SuspendProcessesResponseType extends AutoScalingBackendMessage {
  public SuspendProcessesResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class AutoScalingNotificationTypes extends EucalyptusData {
  public AutoScalingNotificationTypes() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class TerminateInstanceInAutoScalingGroupType extends AutoScalingBackendMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_INSTANCE)
  String instanceId
  @Nonnull
  Boolean shouldDecrementDesiredCapacity
  public TerminateInstanceInAutoScalingGroupType() {  }
}
public class ErrorResponse extends AutoScalingBackendMessage {
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
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.ELB_NAME)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class PolicyNames extends EucalyptusData {
  public PolicyNames() {  }
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class DescribeTerminationPolicyTypesType extends AutoScalingBackendMessage {
  public DescribeTerminationPolicyTypesType() {  }
}
public class DeleteTagsResponseType extends AutoScalingBackendMessage {
  public DeleteTagsResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class SetInstanceHealthResponseType extends AutoScalingBackendMessage {
  public SetInstanceHealthResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DeleteAutoScalingGroupType extends AutoScalingBackendMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  Boolean forceDelete
  public DeleteAutoScalingGroupType() {  }
}
public class DescribeNotificationConfigurationsType extends AutoScalingBackendMessage {
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
public class PutScheduledUpdateGroupActionResponseType extends AutoScalingBackendMessage {
  public PutScheduledUpdateGroupActionResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class ProcessType extends EucalyptusData {
  String processName
  public ProcessType() {  }
  public ProcessType( String processName ) {
    this.processName = processName
  }
}
public class TagDescription extends EucalyptusData {
  String resourceId
  String resourceType
  String key
  String value
  Boolean propagateAtLaunch
  public TagDescription() {  }
}
public class DeleteNotificationConfigurationType extends AutoScalingBackendMessage {
  String autoScalingGroupName
  String topicARN
  public DeleteNotificationConfigurationType() {  }
}
public class ExecutePolicyType extends AutoScalingBackendMessage {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
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
public class DeletePolicyType extends AutoScalingBackendMessage {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String policyName
  public DeletePolicyType() {  }
}
public class DescribeAutoScalingGroupsResult extends EucalyptusData {
  AutoScalingGroupsType autoScalingGroups = new AutoScalingGroupsType()
  String nextToken
  public DescribeAutoScalingGroupsResult() {  }
}
public class CreateLaunchConfigurationResponseType extends AutoScalingBackendMessage {
  public CreateLaunchConfigurationResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class TerminateInstanceInAutoScalingGroupResponseType extends AutoScalingBackendMessage {
  public TerminateInstanceInAutoScalingGroupResponseType() {  }
  TerminateInstanceInAutoScalingGroupResult terminateInstanceInAutoScalingGroupResult = new TerminateInstanceInAutoScalingGroupResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeAutoScalingInstancesResponseType extends AutoScalingBackendMessage {
  public DescribeAutoScalingInstancesResponseType() {  }
  DescribeAutoScalingInstancesResult describeAutoScalingInstancesResult = new DescribeAutoScalingInstancesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class PutNotificationConfigurationType extends AutoScalingBackendMessage {
  String autoScalingGroupName
  String topicARN
  @HttpEmbedded
  AutoScalingNotificationTypes notificationTypes
  public PutNotificationConfigurationType() {  }
}
public class MetricCollectionTypes extends EucalyptusData {
  public MetricCollectionTypes() {  }
  public MetricCollectionTypes( Collection<String> types ) {
    member.addAll( types.collect{ String type -> new MetricCollectionType( metric: type ) } )
  }
  ArrayList<MetricCollectionType> member = new ArrayList<MetricCollectionType>()
}
public class CreateAutoScalingGroupResponseType extends AutoScalingBackendMessage {
  public CreateAutoScalingGroupResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DeleteNotificationConfigurationResponseType extends AutoScalingBackendMessage {
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
public class DescribeScalingProcessTypesResponseType extends AutoScalingBackendMessage {
  public DescribeScalingProcessTypesResponseType() {  }
  DescribeScalingProcessTypesResult describeScalingProcessTypesResult = new DescribeScalingProcessTypesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class PutNotificationConfigurationResponseType extends AutoScalingBackendMessage {
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

  public static Function<Activity, String> startTime() {
    { Activity activity -> activity.startTime } as Function<Activity, String>
  }
}
public class SuspendedProcesses extends EucalyptusData {
  public SuspendedProcesses() {  }
  ArrayList<SuspendedProcessType> member = new ArrayList<SuspendedProcessType>()
}
public class InstanceMonitoring extends EucalyptusData {
  Boolean enabled
  public InstanceMonitoring() {  }
  public InstanceMonitoring( Boolean enabled ) {
    this.enabled = enabled
  }
}
public class DescribeScheduledActionsType extends AutoScalingBackendMessage {
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
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.TAG_FILTER)
  String name
  @HttpEmbedded
  Values values
  public Filter() {  }
  public List<String> values() {
    values != null ?
        values.getMember() :
        []
  }
}
public class ErrorDetail extends EucalyptusData {
  public ErrorDetail() {  }
}
public class Alarms extends EucalyptusData {
  public Alarms() {  }
  public Alarms( Iterable<String> alarmArns ) {
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
public class DescribeAutoScalingInstancesResult extends EucalyptusData {
  AutoScalingInstances autoScalingInstances = new AutoScalingInstances()
  String nextToken
  public DescribeAutoScalingInstancesResult() {  }
}
public class DescribeLaunchConfigurationsType extends AutoScalingBackendMessage {
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
public class DescribeMetricCollectionTypesResponseType extends AutoScalingBackendMessage {
  public DescribeMetricCollectionTypesResponseType() {  }
  DescribeMetricCollectionTypesResult describeMetricCollectionTypesResult = new DescribeMetricCollectionTypesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class AutoScalingInstances extends EucalyptusData {
  public AutoScalingInstances() {  }
  ArrayList<AutoScalingInstanceDetails> member = new ArrayList<AutoScalingInstanceDetails>()
}
public class DescribeTagsType extends AutoScalingBackendMessage {
  @HttpEmbedded
  Filters filters
  String nextToken
  Integer maxRecords
  public DescribeTagsType() {  }
  public List<Filter> filters() {
    filters != null ?
        filters.member :
        []
  }
}
public class AdjustmentType extends EucalyptusData {
  String adjustmentType
  public AdjustmentType() {  }
}
public class DeleteScheduledActionType extends AutoScalingBackendMessage {
  String autoScalingGroupName
  String scheduledActionName
  public DeleteScheduledActionType() {  }
}
public class DisableMetricsCollectionResponseType extends AutoScalingBackendMessage {
  public DisableMetricsCollectionResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class CreateAutoScalingGroupType extends AutoScalingBackendMessage {
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
  @Nonnull @HttpEmbedded
  AvailabilityZones availabilityZones
  @HttpEmbedded
  LoadBalancerNames loadBalancerNames
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.HEALTH_CHECK)
  String healthCheckType
  @AutoScalingMessageValidation.FieldRange
  Integer healthCheckGracePeriod
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  String placementGroup
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
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
    errors
  }
}
public class DisableMetricsCollectionType extends AutoScalingBackendMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  @HttpEmbedded
  Metrics metrics
  public DisableMetricsCollectionType() {  }
}
public class DescribeAdjustmentTypesType extends AutoScalingBackendMessage {
  public DescribeAdjustmentTypesType() {  }
}
public class TerminationPolicies extends EucalyptusData {
  public TerminationPolicies() {  }
  public TerminationPolicies( Collection<String> terminationPolicies ) {
    if ( terminationPolicies != null ) member.addAll( terminationPolicies )
  }
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.TERMINATION_POLICY)
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
public class DescribeNotificationConfigurationsResponseType extends AutoScalingBackendMessage {
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
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class DescribeScalingActivitiesResult extends EucalyptusData {
  Activities activities = new Activities()
  String nextToken
  public DescribeScalingActivitiesResult() {  }
}
public class DescribeAutoScalingNotificationTypesType extends AutoScalingBackendMessage {
  public DescribeAutoScalingNotificationTypesType() {  }
}
public class Metrics extends EucalyptusData {
  public Metrics() {  }
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.METRIC)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class DeleteScheduledActionResponseType extends AutoScalingBackendMessage {
  public DeleteScheduledActionResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeNotificationConfigurationsResult extends EucalyptusData {
  NotificationConfigurations notificationConfigurations = new NotificationConfigurations()
  String nextToken
  public DescribeNotificationConfigurationsResult() {  }
}
public class EnableMetricsCollectionType extends AutoScalingBackendMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  @HttpEmbedded
  Metrics metrics
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.METRIC_GRANULARITY)
  String granularity
  public EnableMetricsCollectionType() {  }
}
public class EnableMetricsCollectionResponseType extends AutoScalingBackendMessage {
  public EnableMetricsCollectionResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class PutScheduledUpdateGroupActionType extends AutoScalingBackendMessage {
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
public class SuspendedProcessType extends EucalyptusData {
  String processName
  String suspensionReason
  public SuspendedProcessType() {  }
}
public class SecurityGroups extends EucalyptusData {
  public SecurityGroups() {  }
  public SecurityGroups( Collection<String> groups ) {
    member.addAll( groups )
  }
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class NotificationConfigurations extends EucalyptusData {
  public NotificationConfigurations() {  }
  ArrayList<NotificationConfiguration> member = new ArrayList<NotificationConfiguration>()
}
public class DeleteLaunchConfigurationResponseType extends AutoScalingBackendMessage {
  public DeleteLaunchConfigurationResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeScheduledActionsResponseType extends AutoScalingBackendMessage {
  public DescribeScheduledActionsResponseType() {  }
  DescribeScheduledActionsResult describeScheduledActionsResult = new DescribeScheduledActionsResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class Filters extends EucalyptusData {
  public Filters() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<Filter> member = new ArrayList<Filter>()
}
public class ResumeProcessesType extends AutoScalingBackendMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  @HttpEmbedded
  ProcessNames scalingProcesses
  public ResumeProcessesType() {  }
}
public class DescribeAdjustmentTypesResponseType extends AutoScalingBackendMessage {
  public DescribeAdjustmentTypesResponseType() {  }
  DescribeAdjustmentTypesResult describeAdjustmentTypesResult = new DescribeAdjustmentTypesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class InstanceIds extends EucalyptusData {
  public InstanceIds() {  }
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_INSTANCE_VERBOSE)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class SuspendProcessesType extends AutoScalingBackendMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
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
public class DescribeAutoScalingInstancesType extends AutoScalingBackendMessage {
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
public class DeleteTagsType extends AutoScalingBackendMessage {
  @Nonnull
  @HttpEmbedded
  Tags tags
  public DeleteTagsType() {  }
}
public class UpdateAutoScalingGroupResponseType extends AutoScalingBackendMessage {
  public UpdateAutoScalingGroupResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class EnabledMetric extends EucalyptusData {
  String metric
  String granularity = "1Minute"
  public EnabledMetric() {  }
}
public class DescribePoliciesResponseType extends AutoScalingBackendMessage {
  public DescribePoliciesResponseType() {  }
  DescribePoliciesResult describePoliciesResult = new DescribePoliciesResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class TagType extends EucalyptusData {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME)
  String resourceId
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.TAG_RESOURCE)
  String resourceType
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.STRING_128)
  String key
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.STRING_256)
  String value
  Boolean propagateAtLaunch
  public TagType() {  }
}
public class DescribeTagsResponseType extends AutoScalingBackendMessage {
  public DescribeTagsResponseType() {  }
  DescribeTagsResult describeTagsResult = new DescribeTagsResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class ScheduledUpdateGroupActions extends EucalyptusData {
  public ScheduledUpdateGroupActions() {  }
  ArrayList<ScheduledUpdateGroupAction> member = new ArrayList<ScheduledUpdateGroupAction>()
}
public class DeletePolicyResponseType extends AutoScalingBackendMessage {
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
public class ExecutePolicyResponseType extends AutoScalingBackendMessage {
  public ExecutePolicyResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class ActivityIds extends EucalyptusData {
  public ActivityIds() {  }
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.UUID_VERBOSE)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class MetricGranularityType extends EucalyptusData {
  String granularity
  public MetricGranularityType() {  }
}
public class AdjustmentTypes extends EucalyptusData {
  public AdjustmentTypes() {  }
  public AdjustmentTypes( Collection<String> values ) {
    member = values.collect { String value ->
      new AdjustmentType( adjustmentType: value )
    } as ArrayList<AdjustmentType>
  }
  ArrayList<AdjustmentType> member = new ArrayList<AdjustmentType>()
}
public class PutScalingPolicyResponseType extends AutoScalingBackendMessage {
  public PutScalingPolicyResponseType() {  }
  PutScalingPolicyResult putScalingPolicyResult = new PutScalingPolicyResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class Tags extends EucalyptusData {
  public Tags() {  }
  @HttpParameterMapping(parameter="member")
  @HttpEmbedded(multiple=true)
  ArrayList<TagType> member = new ArrayList<TagType>()
}
public class SetDesiredCapacityResponseType extends AutoScalingBackendMessage {
  public SetDesiredCapacityResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeScalingActivitiesType extends AutoScalingBackendMessage {
  @HttpEmbedded
  ActivityIds activityIds
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  Integer maxRecords
  String nextToken
  public DescribeScalingActivitiesType() {  }
  public List<String> activityIds() {
    List<String> ids = Lists.newArrayList()
    if ( activityIds != null ) {
      ids = activityIds.getMember()
    }
    return ids
  }
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
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_SNAPSHOT)
  String snapshotId
  @AutoScalingMessageValidation.FieldRange
  Integer volumeSize
  public Ebs() {  }
}
public class SetInstanceHealthType extends AutoScalingBackendMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_INSTANCE)
  String instanceId
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.HEALTH_STATUS)
  String healthStatus
  Boolean shouldRespectGracePeriod
  public SetInstanceHealthType() {  }
}
public class UpdateAutoScalingGroupType extends AutoScalingBackendMessage {
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
  @HttpEmbedded
  AvailabilityZones availabilityZones
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.HEALTH_CHECK)
  String healthCheckType
  @AutoScalingMessageValidation.FieldRange
  Integer healthCheckGracePeriod
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  String placementGroup
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
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
public class DescribeMetricCollectionTypesResult extends EucalyptusData {
  MetricCollectionTypes metrics
  MetricGranularityTypes granularities = new MetricGranularityTypes()
  public DescribeMetricCollectionTypesResult() {  }
}
public class BlockDeviceMappingType extends EucalyptusData {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  String virtualName
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
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
public class DescribeTerminationPolicyTypesResponseType extends AutoScalingBackendMessage {
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
  public EnabledMetrics( Collection<String> enabledMetrics ) {
    if ( enabledMetrics != null ) member.addAll( enabledMetrics.collect{ String metric -> new EnabledMetric(metric: metric) } )
  }
  ArrayList<EnabledMetric> member = new ArrayList<EnabledMetric>()
}
public class SetDesiredCapacityType extends AutoScalingBackendMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String autoScalingGroupName
  @Nonnull @AutoScalingMessageValidation.FieldRange
  Integer desiredCapacity
  Boolean honorCooldown
  public SetDesiredCapacityType() {  }
}
public class PutScalingPolicyType extends AutoScalingBackendMessage {
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
  public PutScalingPolicyType() {  }
}
public class DescribeAutoScalingGroupsResponseType extends AutoScalingBackendMessage {
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

  public static Function<AutoScalingGroupType, String> groupName() {
    { AutoScalingGroupType group -> group.autoScalingGroupName } as Function<AutoScalingGroupType, String>
  }
}
public class DeleteLaunchConfigurationType extends AutoScalingBackendMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  String launchConfigurationName
  public DeleteLaunchConfigurationType() {  }
}
public class DeleteAutoScalingGroupResponseType extends AutoScalingBackendMessage {
  public DeleteAutoScalingGroupResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeScalingActivitiesResponseType extends AutoScalingBackendMessage {
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

  public static Function<ScalingPolicyType, String> policyArn() {
    { ScalingPolicyType policy -> policy.policyARN } as Function<ScalingPolicyType, String>
  }
}
public class AutoScalingGroupNames extends EucalyptusData {
  public AutoScalingGroupNames() {  }
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class Values extends EucalyptusData {
  public Values() {  }
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.STRING_256)
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
public class CreateOrUpdateTagsResponseType extends AutoScalingBackendMessage {
  public CreateOrUpdateTagsResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class Activities extends EucalyptusData {
  public Activities() {  }
  ArrayList<Activity> member = new ArrayList<Activity>()
}
public class DescribePoliciesType extends AutoScalingBackendMessage {
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN)
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
public class ResumeProcessesResponseType extends AutoScalingBackendMessage {
  public ResumeProcessesResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeAutoScalingGroupsType extends AutoScalingBackendMessage {
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
public class CreateLaunchConfigurationType extends AutoScalingBackendMessage {
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.NAME)
  String launchConfigurationName
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_MACHINE_IMAGE)
  String imageId
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  String keyName
  @HttpEmbedded
  SecurityGroups securityGroups
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_USERDATA)
  String userData
  @Nonnull @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_NAME)
  String instanceType
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_KERNEL_IMAGE)
  String kernelId
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_RAMDISK_IMAGE)
  String ramdiskId
  @HttpEmbedded
  BlockDeviceMappings blockDeviceMappings
  @HttpEmbedded
  InstanceMonitoring instanceMonitoring
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.EC2_SPOT_PRICE)
  String spotPrice
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.IAM_NAME_OR_ARN)
  String iamInstanceProfile
  Boolean ebsOptimized
  public CreateLaunchConfigurationType() {  }

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
public class MetricCollectionType extends EucalyptusData {
  String metric
  public MetricCollectionType() {  }
}
public class DescribeLaunchConfigurationsResponseType extends AutoScalingBackendMessage {
  public DescribeLaunchConfigurationsResponseType() {  }
  DescribeLaunchConfigurationsResult describeLaunchConfigurationsResult = new DescribeLaunchConfigurationsResult()
  ResponseMetadata responseMetadata = new ResponseMetadata()
}
public class DescribeScalingProcessTypesType extends AutoScalingBackendMessage {
  public DescribeScalingProcessTypesType() {  }
}
public class CreateOrUpdateTagsType extends AutoScalingBackendMessage {
  @Nonnull @HttpEmbedded
  Tags tags
  public CreateOrUpdateTagsType() {  }
}
public class ProcessNames extends EucalyptusData {
  public ProcessNames() {  }
  @AutoScalingMessageValidation.FieldRegex(AutoScalingMessageValidation.FieldRegexValue.SCALING_PROCESS)
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>()
}
public class DescribeAdjustmentTypesResult extends EucalyptusData {
  AdjustmentTypes adjustmentTypes
  public DescribeAdjustmentTypesResult() {  }
}
public class DescribeScalingProcessTypesResult extends EucalyptusData {
  Processes processes = new Processes()
  public DescribeScalingProcessTypesResult() {  }
}
public class DescribeAutoScalingNotificationTypesResult extends EucalyptusData {
  AutoScalingNotificationTypes autoScalingNotificationTypes
  public DescribeAutoScalingNotificationTypesResult() {  }
}
