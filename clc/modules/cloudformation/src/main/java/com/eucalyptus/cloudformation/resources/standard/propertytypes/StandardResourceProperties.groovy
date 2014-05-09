/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.cloudformation.resources.standard.propertytypes

import com.eucalyptus.cloudformation.resources.ResourceProperties
import com.eucalyptus.cloudformation.resources.annotations.Property
import com.eucalyptus.cloudformation.resources.annotations.Required
import com.fasterxml.jackson.databind.JsonNode
import com.google.common.collect.Lists
import groovy.transform.ToString

@ToString(includeNames=true)
public class AWSAutoScalingAutoScalingGroupProperties implements ResourceProperties {
  @Required
  @Property
  List<String> availabilityZones = Lists.newArrayList();
  @Property
  Integer cooldown;
  @Property
  Integer desiredCapacity;
  @Property
  Integer healthCheckGracePeriod;
  @Property
  String healthCheckType;
  @Property
  String instanceId;
  @Property
  String launchConfigurationName;
  @Property
  List<String> loadBalancerNames = Lists.newArrayList();
  @Required
  @Property
  Integer maxSize;
  @Required
  @Property
  Integer minSize;
  @Property
  AutoScalingNotificationConfiguration notificationConfiguration;
  @Property
  List<AutoScalingTag> tags = Lists.newArrayList();
  @Property
  List<String> terminationPolicies = Lists.newArrayList();
  @Property(name="VPCZoneIdentifier")
  List<String> vpcZoneIdentifier = Lists.newArrayList();
}

@ToString(includeNames=true)
public class AWSAutoScalingLaunchConfigurationProperties implements ResourceProperties {
  @Property
  Boolean associatePublicIpAddress;
  @Property
  List<AutoScalingBlockDeviceMapping> blockDeviceMappings = Lists.newArrayList();
  @Property
  Boolean ebsOptimized;
  @Property
  String iamInstanceProfile;
  @Required
  @Property
  String imageId;
  @Property
  String instanceId;
  @Property
  Boolean instanceMonitoring;
  @Required
  @Property
  String instanceType;
  @Property
  String kernelId;
  @Property
  String keyName;
  @Property
  String ramDiskId;
  @Property
  List<String> securityGroups = Lists.newArrayList();
  @Property
  String spotPrice;
  @Property
  String userData;
}

@ToString(includeNames=true)
public class AWSAutoScalingScalingPolicyProperties implements ResourceProperties {
  @Required
  @Property
  String adjustmentType;
  @Required
  @Property
  String autoScalingGroupName;
  @Property
  Integer cooldown;
  @Required
  @Property
  Integer scalingAdjustment;
}

@ToString(includeNames=true)
public class AWSAutoScalingScheduledActionProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSAutoScalingTriggerProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSCloudFormationAuthenticationProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSCloudFormationCustomResourceProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSCloudFormationInitProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSCloudFormationStackProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSCloudFormationWaitConditionProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSCloudFormationWaitConditionHandleProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSCloudFrontDistributionProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSCloudWatchAlarmProperties implements ResourceProperties {
  @Property
  Boolean actionsEnabled;
  @Property
  List<String> alarmActions = Lists.newArrayList();
  @Property
  String alarmDescription;
  @Property
  String alarmName;
  @Required
  @Property
  String comparisonOperator;
  @Property
  List<CloudWatchMetricDimension> dimensions = Lists.newArrayList();
  @Required
  @Property
  Integer evaluationPeriods;
  @Property
  List<String> insufficientDataActions = Lists.newArrayList();
  @Required
  @Property
  String metricName;
  @Required
  @Property
  String namespace;
  @Property(name="OKActions")
  List<String> okActions = Lists.newArrayList();
  @Required
  @Property
  Integer period;
  @Required
  @Property
  String statistic;
  @Required
  @Property
  Double threshold;
  @Property
  String unit;
}

@ToString(includeNames=true)
public class AWSDynamoDBTableProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2CustomerGatewayProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2DHCPOptionsProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2EIPProperties implements ResourceProperties {
  @Property
  String instanceId;
  @Property
  String domain;
}

@ToString(includeNames=true)
public class AWSEC2EIPAssociationProperties implements ResourceProperties {
  @Property
  String allocationId;
  @Property(name="EIP")
  String eip;
  @Property
  String instanceId;
  @Property
  String networkInterfaceId;
  @Property
  String privateIpAddress;
}

@ToString(includeNames=true)
public class AWSEC2InstanceProperties implements ResourceProperties {
  @Property
  String availabilityZone;
  @Property
  List<EC2BlockDeviceMapping> blockDeviceMappings = Lists.newArrayList();
  @Property
  Boolean  disableApiTermination;
  @Property
  Boolean ebsOptimized;
  @Property
  String iamInstanceProfile;
  @Property
  @Required
  String imageId;
  @Property
  String instanceType;
  @Property
  String kernelId;
  @Property
  String keyName;
  @Property
  Boolean monitoring;
  @Property
  List<EC2NetworkInterface>  networkInterfaces = Lists.newArrayList();
  @Property
  String placementGroupName;
  @Property
  String privateIpAddress;
  @Property
  String ramdiskId;
  @Property
  List<String> securityGroupIds = Lists.newArrayList();
  @Property
  List<String> securityGroups = Lists.newArrayList();
  @Property
  Boolean sourceDestCheck;
  @Property
  String subnetId;
  @Property
  List<EC2Tag> tags = Lists.newArrayList();
  @Property
  String tenancy;
  @Property
  String userData;
  @Property
  List<EC2MountPoint> volumes = Lists.newArrayList();
}

@ToString(includeNames=true)
public class AWSEC2InternetGatewayProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2NetworkAclProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2NetworkAclEntryProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2NetworkInterfaceProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2NetworkInterfaceAttachmentProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2RouteProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2RouteTableProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2SecurityGroupProperties implements ResourceProperties {
  @Property
  @Required
  String groupDescription;
  @Property
  List<EC2SecurityGroupRule> securityGroupEgress = Lists.newArrayList();
  @Property
  List<EC2SecurityGroupRule> securityGroupIngress = Lists.newArrayList();
  @Property
  String vpcId;
  @Property
  List<EC2Tag> tags = Lists.newArrayList();
}

@ToString(includeNames=true)
public class AWSEC2SecurityGroupIngressProperties implements ResourceProperties {
  @Property
  String groupName;
  @Property
  String groupId;
  @Required
  @Property
  String ipProtocol;
  @Property
  String cidrIp;
  @Property
  String sourceSecurityGroupName;
  @Property
  String sourceSecurityGroupId;
  @Property
  String sourceSecurityGroupOwnerId;
  @Required
  @Property
  String fromPort;
  @Required // not required for ip protocol other than ICMP, TCP, UDP but we don't support VPC currently
  @Property
  String toPort;
}

@ToString(includeNames=true)
public class AWSEC2SecurityGroupEgressProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2SubnetProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2SubnetNetworkAclAssociationProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2SubnetRouteTableAssociationProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2VolumeProperties implements ResourceProperties {
  @Required
  @Property
  String availabilityZone;
  @Property
  Integer iops;
  @Property
  String size;
  @Property
  String snapshotId;
  @Property
  List<EC2Tag> tags = Lists.newArrayList();
  @Property
  String volumeType;
}

@ToString(includeNames=true)
public class AWSEC2VolumeAttachmentProperties implements ResourceProperties {
  @Required
  @Property
  String device;
  @Required
  @Property
  String instanceId;
  @Required
  @Property
  String volumeId;
}

@ToString(includeNames=true)
public class AWSEC2VPCProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2VPCDHCPOptionsAssociationProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2VPCGatewayAttachmentProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2VPNConnectionProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2VPNConnectionRouteProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2VPNGatewayProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSEC2VPNGatewayRoutePropagationProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSElastiCacheCacheClusterProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSElastiCacheParameterGroupProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSElastiCacheSecurityGroupProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSElastiCacheSecurityGroupIngressProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSElastiCacheSubnetGroupProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSElasticBeanstalkApplicationProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSElasticBeanstalkApplicationVersionProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSElasticBeanstalkConfigurationTemplateProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSElasticBeanstalkEnvironmentProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSElasticLoadBalancingLoadBalancerProperties implements ResourceProperties {
  @Property
  ElasticLoadBalancingAccessLoggingPolicy accessLoggingPolicy;
  @Property
  ElasticLoadBalancingAppCookieStickinessPolicy appCookieStickinessPolicy;
  @Property
  List<String> availabilityZones = Lists.newArrayList();
  @Property
  ElasticLoadBalancingConnectionDrainingPolicy connectionDrainingPolicy;
  @Property
  Boolean crossZone;
  @Property
  ElasticLoadBalancingHealthCheckType healthCheck;
  @Property
  List<String> instances = Lists.newArrayList();
  @Property(name="LBCookieStickinessPolicy")
  ElasticLoadBalancingLBCookieStickinessPolicyType lbCookieStickinessPolicy;
  @Property
  String loadBalancerName;
  @Required
  @Property
  List<ElasticLoadBalancingListener> listeners = Lists.newArrayList();
  @Property
  List<ElasticLoadBalancingPolicyType> policies = Lists.newArrayList();
  @Property
  String scheme;
  @Property
  List<String> securityGroups = Lists.newArrayList();
  @Property
  List<String> subnets = Lists.newArrayList();
}
@ToString(includeNames=true)
public class AWSIAMAccessKeyProperties implements ResourceProperties {
  @Property
  Integer serial;
  // @Required -- docs say required but many examples in docs do not have it...examples win
  @Property
  String status;
  @Required
  @Property
  String userName;
}

@ToString(includeNames=true)
public class AWSIAMGroupProperties implements ResourceProperties {
  @Property
  String path;
  @Property
  List<EmbeddedIAMPolicy> policies = Lists.newArrayList();
}

@ToString(includeNames=true)
public class AWSIAMInstanceProfileProperties implements ResourceProperties {
  @Required
  @Property
  String path;
  @Required
  @Property
  List<String> roles = Lists.newArrayList();
}

@ToString(includeNames=true)
public class AWSIAMPolicyProperties implements ResourceProperties {
  @Property
  List<String> groups = Lists.newArrayList();
  @Required
  @Property
  JsonNode policyDocument;
  @Required
  @Property
  String policyName;
  @Property
  List<String> roles = Lists.newArrayList();
  @Property
  List<String> users = Lists.newArrayList();
}

@ToString(includeNames=true)
public class AWSIAMRoleProperties implements ResourceProperties {
  @Required
  @Property
  JsonNode assumeRolePolicyDocument;
  @Property
  String path;
  @Property
  List<EmbeddedIAMPolicy> policies = Lists.newArrayList();
}

@ToString(includeNames=true)
public class AWSIAMUserProperties implements ResourceProperties {
  @Property
  String path;
  @Property
  List<String> groups = Lists.newArrayList();
  @Property
  LoginProfile loginProfile;
  @Property
  List<EmbeddedIAMPolicy> policies = Lists.newArrayList();
}

@ToString(includeNames=true)
public class AWSIAMUserToGroupAdditionProperties implements ResourceProperties {
  @Required
  @Property
  String groupName;
  @Required
  @Property
  List<String> users = Lists.newArrayList();
}

@ToString(includeNames=true)
public class AWSRedshiftClusterProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSRedshiftClusterParameterGroupProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSRedshiftClusterSecurityGroupProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSRedshiftClusterSecurityGroupIngressProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSRedshiftClusterSubnetGroupProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSRDSDBInstanceProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSRDSDBParameterGroupProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSRDSDBSubnetGroupProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSRDSDBSecurityGroupProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSRDSDBSecurityGroupIngressProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSRoute53RecordSetProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSRoute53RecordSetGroupProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSS3BucketProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSS3BucketPolicyProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSSDBDomainProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSSNSTopicPolicyProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSSNSTopicProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSSQSQueueProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSSQSQueuePolicyProperties implements ResourceProperties {
}

