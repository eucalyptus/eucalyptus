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

package com.eucalyptus.cloudformation.resources.standard.info

import com.eucalyptus.cloudformation.resources.ResourceInfo
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson
import groovy.transform.ToString

@ToString(includeNames=true)
public class AWSAutoScalingAutoScalingGroupResourceInfo extends ResourceInfo {
  public AWSAutoScalingAutoScalingGroupResourceInfo() {
    setType("AWS::AutoScaling::AutoScalingGroup");
  }
}


@ToString(includeNames=true)
public class AWSAutoScalingLaunchConfigurationResourceInfo extends ResourceInfo {
  public AWSAutoScalingLaunchConfigurationResourceInfo() {
    setType("AWS::AutoScaling::LaunchConfiguration");
  }
}


@ToString(includeNames=true)
public class AWSAutoScalingScalingPolicyResourceInfo extends ResourceInfo {
  public AWSAutoScalingScalingPolicyResourceInfo() {
    setType("AWS::AutoScaling::ScalingPolicy");
  }
}


@ToString(includeNames=true)
public class AWSAutoScalingScheduledActionResourceInfo extends ResourceInfo {
  public AWSAutoScalingScheduledActionResourceInfo() {
    setType("AWS::AutoScaling::ScheduledAction");
  }
}


@ToString(includeNames=true)
public class AWSAutoScalingTriggerResourceInfo extends ResourceInfo {
  public AWSAutoScalingTriggerResourceInfo() {
    setType("AWS::AutoScaling::Trigger");
  }
}


@ToString(includeNames=true)
public class AWSCloudFormationAuthenticationResourceInfo extends ResourceInfo {
  public AWSCloudFormationAuthenticationResourceInfo() {
    setType("AWS::CloudFormation::Authentication");
  }
}


@ToString(includeNames=true)
public class AWSCloudFormationCustomResourceResourceInfo extends ResourceInfo {
  public AWSCloudFormationCustomResourceResourceInfo() {
    setType("AWS::CloudFormation::CustomResource");
  }
  @Override
  public boolean canCheckAttributes() {
    return false;
  }
}


@ToString(includeNames=true)
public class AWSCloudFormationInitResourceInfo extends ResourceInfo {
  public AWSCloudFormationInitResourceInfo() {
    setType("AWS::CloudFormation::Init");
  }
}


@ToString(includeNames=true)
public class AWSCloudFormationStackResourceInfo extends ResourceInfo {
  public AWSCloudFormationStackResourceInfo() {
    setType("AWS::CloudFormation::Stack");
  }
  @Override
  public boolean canCheckAttributes() {
    return false;
  }
  @Override
  public Collection<String> getRequiredCapabilities() {
    ArrayList<String> capabilities = new ArrayList<String>();
    capabilities.add("CAPABILITY_IAM");
    return capabilities;
  }
}


@ToString(includeNames=true)
public class AWSCloudFormationWaitConditionResourceInfo extends ResourceInfo {
  public AWSCloudFormationWaitConditionResourceInfo() {
    setType("AWS::CloudFormation::WaitCondition");
  }
}


@ToString(includeNames=true)
public class AWSCloudFormationWaitConditionHandleResourceInfo extends ResourceInfo {
  public AWSCloudFormationWaitConditionHandleResourceInfo() {
    setType("AWS::CloudFormation::WaitConditionHandle");
  }
}


@ToString(includeNames=true)
public class AWSCloudFrontDistributionResourceInfo extends ResourceInfo {
  public AWSCloudFrontDistributionResourceInfo() {
    setType("AWS::CloudFront::Distribution");
  }
}


@ToString(includeNames=true)
public class AWSCloudWatchAlarmResourceInfo extends ResourceInfo {
  public AWSCloudWatchAlarmResourceInfo() {
    setType("AWS::CloudWatch::Alarm");
  }
}


@ToString(includeNames=true)
public class AWSDynamoDBTableResourceInfo extends ResourceInfo {
  public AWSDynamoDBTableResourceInfo() {
    setType("AWS::DynamoDB::Table");
  }
}


@ToString(includeNames=true)
public class AWSEC2CustomerGatewayResourceInfo extends ResourceInfo {
  public AWSEC2CustomerGatewayResourceInfo() {
    setType("AWS::EC2::CustomerGateway");
  }
}


@ToString(includeNames=true)
public class AWSEC2DHCPOptionsResourceInfo extends ResourceInfo {
  public AWSEC2DHCPOptionsResourceInfo() {
    setType("AWS::EC2::DHCPOptions");
  }
}


@ToString(includeNames=true)
public class AWSEC2EIPResourceInfo extends ResourceInfo {
  public AWSEC2EIPResourceInfo() {
    setType("AWS::EC2::EIP");
  }
}


@ToString(includeNames=true)
public class AWSEC2EIPAssociationResourceInfo extends ResourceInfo {
  public AWSEC2EIPAssociationResourceInfo() {
    setType("AWS::EC2::EIPAssociation");
  }
}


@ToString(includeNames=true)
public class AWSEC2InstanceResourceInfo extends ResourceInfo {
  @AttributeJson
  String availabilityZone;
  @AttributeJson
  String privateDnsName;
  @AttributeJson
  String publicDnsName;
  @AttributeJson
  String privateIp;
  @AttributeJson
  String publicIp;

  public AWSEC2InstanceResourceInfo() {
    setType("AWS::EC2::Instance");
  }
}


@ToString(includeNames=true)
public class AWSEC2InternetGatewayResourceInfo extends ResourceInfo {
  public AWSEC2InternetGatewayResourceInfo() {
    setType("AWS::EC2::InternetGateway");
  }
}


@ToString(includeNames=true)
public class AWSEC2NetworkAclResourceInfo extends ResourceInfo {
  public AWSEC2NetworkAclResourceInfo() {
    setType("AWS::EC2::NetworkAcl");
  }
}


@ToString(includeNames=true)
public class AWSEC2NetworkAclEntryResourceInfo extends ResourceInfo {
  public AWSEC2NetworkAclEntryResourceInfo() {
    setType("AWS::EC2::NetworkAclEntry");
  }
}


@ToString(includeNames=true)
public class AWSEC2NetworkInterfaceResourceInfo extends ResourceInfo {
  public AWSEC2NetworkInterfaceResourceInfo() {
    setType("AWS::EC2::NetworkInterface");
  }
}


@ToString(includeNames=true)
public class AWSEC2NetworkInterfaceAttachmentResourceInfo extends ResourceInfo {
  public AWSEC2NetworkInterfaceAttachmentResourceInfo() {
    setType("AWS::EC2::NetworkInterfaceAttachment");
  }
}


@ToString(includeNames=true)
public class AWSEC2RouteResourceInfo extends ResourceInfo {
  public AWSEC2RouteResourceInfo() {
    setType("AWS::EC2::Route");
  }
}


@ToString(includeNames=true)
public class AWSEC2RouteTableResourceInfo extends ResourceInfo {
  public AWSEC2RouteTableResourceInfo() {
    setType("AWS::EC2::RouteTable");
  }
}


@ToString(includeNames=true)
public class AWSEC2SecurityGroupResourceInfo extends ResourceInfo {
  @AttributeJson
  String groupId;
  public AWSEC2SecurityGroupResourceInfo() {
    setType("AWS::EC2::SecurityGroup");
  }
}


@ToString(includeNames=true)
public class AWSEC2SecurityGroupIngressResourceInfo extends ResourceInfo {
  public AWSEC2SecurityGroupIngressResourceInfo() {
    setType("AWS::EC2::SecurityGroupIngress");
  }
}


@ToString(includeNames=true)
public class AWSEC2SecurityGroupEgressResourceInfo extends ResourceInfo {
  public AWSEC2SecurityGroupEgressResourceInfo() {
    setType("AWS::EC2::SecurityGroupEgress");
  }
}


@ToString(includeNames=true)
public class AWSEC2SubnetResourceInfo extends ResourceInfo {
  public AWSEC2SubnetResourceInfo() {
    setType("AWS::EC2::Subnet");
  }
}


@ToString(includeNames=true)
public class AWSEC2SubnetNetworkAclAssociationResourceInfo extends ResourceInfo {
  public AWSEC2SubnetNetworkAclAssociationResourceInfo() {
    setType("AWS::EC2::SubnetNetworkAclAssociation");
  }
}


@ToString(includeNames=true)
public class AWSEC2SubnetRouteTableAssociationResourceInfo extends ResourceInfo {
  public AWSEC2SubnetRouteTableAssociationResourceInfo() {
    setType("AWS::EC2::SubnetRouteTableAssociation");
  }
}


@ToString(includeNames=true)
public class AWSEC2VolumeResourceInfo extends ResourceInfo {
  public AWSEC2VolumeResourceInfo() {
    setType("AWS::EC2::Volume");
  }
  @Override
  public boolean supportsSnapshot() {
    return true;
  }
}


@ToString(includeNames=true)
public class AWSEC2VolumeAttachmentResourceInfo extends ResourceInfo {
  public AWSEC2VolumeAttachmentResourceInfo() {
    setType("AWS::EC2::VolumeAttachment");
  }
}


@ToString(includeNames=true)
public class AWSEC2VPCResourceInfo extends ResourceInfo {
  public AWSEC2VPCResourceInfo() {
    setType("AWS::EC2::VPC");
  }
}


@ToString(includeNames=true)
public class AWSEC2VPCDHCPOptionsAssociationResourceInfo extends ResourceInfo {
  public AWSEC2VPCDHCPOptionsAssociationResourceInfo() {
    setType("AWS::EC2::VPCDHCPOptionsAssociation");
  }
}


@ToString(includeNames=true)
public class AWSEC2VPCGatewayAttachmentResourceInfo extends ResourceInfo {
  public AWSEC2VPCGatewayAttachmentResourceInfo() {
    setType("AWS::EC2::VPCGatewayAttachment");
  }
}


@ToString(includeNames=true)
public class AWSEC2VPNConnectionResourceInfo extends ResourceInfo {
  public AWSEC2VPNConnectionResourceInfo() {
    setType("AWS::EC2::VPNConnection");
  }
}


@ToString(includeNames=true)
public class AWSEC2VPNConnectionRouteResourceInfo extends ResourceInfo {
  public AWSEC2VPNConnectionRouteResourceInfo() {
    setType("AWS::EC2::VPNConnectionRoute");
  }
}


@ToString(includeNames=true)
public class AWSEC2VPNGatewayResourceInfo extends ResourceInfo {
  public AWSEC2VPNGatewayResourceInfo() {
    setType("AWS::EC2::VPNGateway");
  }
}


@ToString(includeNames=true)
public class AWSEC2VPNGatewayRoutePropagationResourceInfo extends ResourceInfo {
  public AWSEC2VPNGatewayRoutePropagationResourceInfo() {
    setType("AWS::EC2::VPNGatewayRoutePropagation");
  }
}


@ToString(includeNames=true)
public class AWSElastiCacheCacheClusterResourceInfo extends ResourceInfo {
  public AWSElastiCacheCacheClusterResourceInfo() {
    setType("AWS::ElastiCache::CacheCluster");
  }
}


@ToString(includeNames=true)
public class AWSElastiCacheParameterGroupResourceInfo extends ResourceInfo {
  public AWSElastiCacheParameterGroupResourceInfo() {
    setType("AWS::ElastiCache::ParameterGroup");
  }
}


@ToString(includeNames=true)
public class AWSElastiCacheSecurityGroupResourceInfo extends ResourceInfo {
  public AWSElastiCacheSecurityGroupResourceInfo() {
    setType("AWS::ElastiCache::SecurityGroup");
  }
}


@ToString(includeNames=true)
public class AWSElastiCacheSecurityGroupIngressResourceInfo extends ResourceInfo {
  public AWSElastiCacheSecurityGroupIngressResourceInfo() {
    setType("AWS::ElastiCache::SecurityGroupIngress");
  }
}


@ToString(includeNames=true)
public class AWSElastiCacheSubnetGroupResourceInfo extends ResourceInfo {
  public AWSElastiCacheSubnetGroupResourceInfo() {
    setType("AWS::ElastiCache::SubnetGroup");
  }
}


@ToString(includeNames=true)
public class AWSElasticBeanstalkApplicationResourceInfo extends ResourceInfo {
  public AWSElasticBeanstalkApplicationResourceInfo() {
    setType("AWS::ElasticBeanstalk::Application");
  }
}


@ToString(includeNames=true)
public class AWSElasticBeanstalkApplicationVersionResourceInfo extends ResourceInfo {
  public AWSElasticBeanstalkApplicationVersionResourceInfo() {
    setType("AWS::ElasticBeanstalk::ApplicationVersion");
  }
}


@ToString(includeNames=true)
public class AWSElasticBeanstalkConfigurationTemplateResourceInfo extends ResourceInfo {
  public AWSElasticBeanstalkConfigurationTemplateResourceInfo() {
    setType("AWS::ElasticBeanstalk::ConfigurationTemplate");
  }
}


@ToString(includeNames=true)
public class AWSElasticBeanstalkEnvironmentResourceInfo extends ResourceInfo {
  public AWSElasticBeanstalkEnvironmentResourceInfo() {
    setType("AWS::ElasticBeanstalk::Environment");
  }
}


@ToString(includeNames=true)
public class AWSElasticLoadBalancingLoadBalancerResourceInfo extends ResourceInfo {
  @AttributeJson
  String canonicalHostedZoneName;
  @AttributeJson
  String canonicalHostedZoneNameID;
  @AttributeJson(name="DNSName")
  String dnsName;
  @AttributeJson(name="SourceSecurityGroup.GroupName")
  String sourceSecurityGroupGroupName;
  @AttributeJson(name="SourceSecurityGroup.OwnerAlias")
  String sourceSecurityGroupOwnerAlias;
  public AWSElasticLoadBalancingLoadBalancerResourceInfo() {
    setType("AWS::ElasticLoadBalancing::LoadBalancer");
  }
}


@ToString(includeNames=true)
public class AWSIAMAccessKeyResourceInfo extends ResourceInfo {
  @AttributeJson
  String secretAccessKey;
  public AWSIAMAccessKeyResourceInfo() {
    setType("AWS::IAM::AccessKey");
  }
  @Override
  public Collection<String> getRequiredCapabilities() {
    ArrayList<String> capabilities = new ArrayList<String>();
    capabilities.add("CAPABILITY_IAM");
    return capabilities;
  }
}


@ToString(includeNames=true)
public class AWSIAMGroupResourceInfo extends ResourceInfo {
  @AttributeJson
  String arn;
  public AWSIAMGroupResourceInfo() {
    setType("AWS::IAM::Group");
  }
  @Override
  public Collection<String> getRequiredCapabilities() {
    ArrayList<String> capabilities = new ArrayList<String>();
    capabilities.add("CAPABILITY_IAM");
    return capabilities;
  }
}


@ToString(includeNames=true)
public class AWSIAMInstanceProfileResourceInfo extends ResourceInfo {
  @AttributeJson
  String arn;
  public AWSIAMInstanceProfileResourceInfo() {
    setType("AWS::IAM::InstanceProfile");
  }
  @Override
  public Collection<String> getRequiredCapabilities() {
    ArrayList<String> capabilities = new ArrayList<String>();
    capabilities.add("CAPABILITY_IAM");
    return capabilities;
  }
}


@ToString(includeNames=true)
public class AWSIAMPolicyResourceInfo extends ResourceInfo {
  public AWSIAMPolicyResourceInfo() {
    setType("AWS::IAM::Policy");
  }
  @Override
  public Collection<String> getRequiredCapabilities() {
    ArrayList<String> capabilities = new ArrayList<String>();
    capabilities.add("CAPABILITY_IAM");
    return capabilities;
  }
}


@ToString(includeNames=true)
public class AWSIAMRoleResourceInfo extends ResourceInfo {
  @AttributeJson
  String arn;
  public AWSIAMRoleResourceInfo() {
    setType("AWS::IAM::Role");
  }
  @Override
  public Collection<String> getRequiredCapabilities() {
    ArrayList<String> capabilities = new ArrayList<String>();
    capabilities.add("CAPABILITY_IAM");
    return capabilities;
  }
}


@ToString(includeNames=true)
public class AWSIAMUserResourceInfo extends ResourceInfo {
  @AttributeJson
  String arn;
  public AWSIAMUserResourceInfo() {
    setType("AWS::IAM::User");
  }
  @Override
  public Collection<String> getRequiredCapabilities() {
    ArrayList<String> capabilities = new ArrayList<String>();
    capabilities.add("CAPABILITY_IAM");
    return capabilities;
  }
}


@ToString(includeNames=true)
public class AWSIAMUserToGroupAdditionResourceInfo extends ResourceInfo {
  public AWSIAMUserToGroupAdditionResourceInfo() {
    setType("AWS::IAM::UserToGroupAddition");
  }

  @Override
  public Collection<String> getRequiredCapabilities() {
    ArrayList<String> capabilities = new ArrayList<String>();
    capabilities.add("CAPABILITY_IAM");
    return capabilities;
  }
}


@ToString(includeNames=true)
public class AWSRedshiftClusterResourceInfo extends ResourceInfo {
  public AWSRedshiftClusterResourceInfo() {
    setType("AWS::Redshift::Cluster");
  }
  @Override
  public boolean supportsSnapshot() {
    return true;
  }
}


@ToString(includeNames=true)
public class AWSRedshiftClusterParameterGroupResourceInfo extends ResourceInfo {
  public AWSRedshiftClusterParameterGroupResourceInfo() {
    setType("AWS::Redshift::ClusterParameterGroup");
  }
}


@ToString(includeNames=true)
public class AWSRedshiftClusterSecurityGroupResourceInfo extends ResourceInfo {
  public AWSRedshiftClusterSecurityGroupResourceInfo() {
    setType("AWS::Redshift::ClusterSecurityGroup");
  }
}


@ToString(includeNames=true)
public class AWSRedshiftClusterSecurityGroupIngressResourceInfo extends ResourceInfo {
  public AWSRedshiftClusterSecurityGroupIngressResourceInfo() {
    setType("AWS::Redshift::ClusterSecurityGroupIngress");
  }
}


@ToString(includeNames=true)
public class AWSRedshiftClusterSubnetGroupResourceInfo extends ResourceInfo {
  public AWSRedshiftClusterSubnetGroupResourceInfo() {
    setType("AWS::Redshift::ClusterSubnetGroup");
  }
}


@ToString(includeNames=true)
public class AWSRDSDBInstanceResourceInfo extends ResourceInfo {
  public AWSRDSDBInstanceResourceInfo() {
    setType("AWS::RDS::DBInstance");
  }
  @Override
  public boolean supportsSnapshot() {
    return true;
  }
}


@ToString(includeNames=true)
public class AWSRDSDBParameterGroupResourceInfo extends ResourceInfo {
  public AWSRDSDBParameterGroupResourceInfo() {
    setType("AWS::RDS::DBParameterGroup");
  }
}


@ToString(includeNames=true)
public class AWSRDSDBSubnetGroupResourceInfo extends ResourceInfo {
  public AWSRDSDBSubnetGroupResourceInfo() {
    setType("AWS::RDS::DBSubnetGroup");
  }
}


@ToString(includeNames=true)
public class AWSRDSDBSecurityGroupResourceInfo extends ResourceInfo {
  public AWSRDSDBSecurityGroupResourceInfo() {
    setType("AWS::RDS::DBSecurityGroup");
  }
}


@ToString(includeNames=true)
public class AWSRDSDBSecurityGroupIngressResourceInfo extends ResourceInfo {
  public AWSRDSDBSecurityGroupIngressResourceInfo() {
    setType("AWS::RDS::DBSecurityGroupIngress");
  }
}


@ToString(includeNames=true)
public class AWSRoute53RecordSetResourceInfo extends ResourceInfo {
  public AWSRoute53RecordSetResourceInfo() {
    setType("AWS::Route53::RecordSet");
  }
}


@ToString(includeNames=true)
public class AWSRoute53RecordSetGroupResourceInfo extends ResourceInfo {
  public AWSRoute53RecordSetGroupResourceInfo() {
    setType("AWS::Route53::RecordSetGroup");
  }
}


@ToString(includeNames=true)
public class AWSS3BucketResourceInfo extends ResourceInfo {
  public AWSS3BucketResourceInfo() {
    setType("AWS::S3::Bucket");
  }
}


@ToString(includeNames=true)
public class AWSS3BucketPolicyResourceInfo extends ResourceInfo {
  public AWSS3BucketPolicyResourceInfo() {
    setType("AWS::S3::BucketPolicy");
  }
}


@ToString(includeNames=true)
public class AWSSDBDomainResourceInfo extends ResourceInfo {
  public AWSSDBDomainResourceInfo() {
    setType("AWS::SDB::Domain");
  }
}


@ToString(includeNames=true)
public class AWSSNSTopicPolicyResourceInfo extends ResourceInfo {
  public AWSSNSTopicPolicyResourceInfo() {
    setType("AWS::SNS::TopicPolicy");
  }
}


@ToString(includeNames=true)
public class AWSSNSTopicResourceInfo extends ResourceInfo {
  public AWSSNSTopicResourceInfo() {
    setType("AWS::SNS::Topic");
  }
}


@ToString(includeNames=true)
public class AWSSQSQueueResourceInfo extends ResourceInfo {
  public AWSSQSQueueResourceInfo() {
    setType("AWS::SQS::Queue");
  }
}


@ToString(includeNames=true)
public class AWSSQSQueuePolicyResourceInfo extends ResourceInfo {
  public AWSSQSQueuePolicyResourceInfo() {
    setType("AWS::SQS::QueuePolicy");
  }
}

