package com.eucalyptus.cloudformation.resources.standard.propertytypes

import com.eucalyptus.cloudformation.resources.ResourceProperties
import com.eucalyptus.cloudformation.resources.annotations.Property
import com.eucalyptus.cloudformation.resources.annotations.Required
import com.fasterxml.jackson.databind.JsonNode
import com.google.common.collect.Lists
import groovy.transform.ToString

@ToString(includeNames=true)
public class AWSAutoScalingAutoScalingGroupProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSAutoScalingLaunchConfigurationProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSAutoScalingScalingPolicyProperties implements ResourceProperties {
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
}

@ToString(includeNames=true)
public class AWSEC2EIPAssociationProperties implements ResourceProperties {
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
}

@ToString(includeNames=true)
public class AWSEC2VolumeAttachmentProperties implements ResourceProperties {
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
}

@ToString(includeNames=true)
public class AWSIAMAccessKeyProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSIAMGroupProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSIAMInstanceProfileProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSIAMPolicyProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSIAMRoleProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSIAMUserProperties implements ResourceProperties {
}

@ToString(includeNames=true)
public class AWSIAMUserToGroupAdditionProperties implements ResourceProperties {
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

