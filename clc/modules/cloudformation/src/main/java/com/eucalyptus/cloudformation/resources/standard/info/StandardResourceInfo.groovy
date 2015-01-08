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

import com.eucalyptus.cloudformation.CloudFormationException
import com.eucalyptus.cloudformation.ValidationErrorException
import com.eucalyptus.cloudformation.resources.ResourceInfo
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson
import com.google.common.collect.Lists
import com.google.common.collect.Maps
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
public class AWSCloudFormationStackResourceInfo extends ResourceInfo {
  public AWSCloudFormationStackResourceInfo() {
    setType("AWS::CloudFormation::Stack");
  }

  @Override
  public Collection<String> getRequiredCapabilities() {
    ArrayList<String> capabilities = new ArrayList<String>();
    capabilities.add("CAPABILITY_IAM");
    return capabilities;
  }

  Map<String, String> outputAttributes = Maps.newLinkedHashMap();

  @Override
  boolean isAttributeAllowed(String attributeName) {
    return attributeName != null && (attributeName.startsWith("Outputs.") || attributeName.startsWith("outputs."));
  }

  @Override
  String getResourceAttributeJson(String attributeName) throws CloudFormationException {
    if (!outputAttributes.containsKey(attributeName)) {
      throw new ValidationErrorException("Stack does not have an attribute named " + attributeName);
    } else {
      return outputAttributes.get(attributeName);
    }
  }

  @Override
  void setResourceAttributeJson(String attributeName, String attributeValueJson) throws CloudFormationException {
    if (attributeName == null || !(attributeName.startsWith("Outputs.") || attributeName.startsWith("outputs."))) {
      throw new ValidationErrorException("Stack can not have an attribute named " + attributeName);
    } else {
      outputAttributes.put(attributeName, attributeValueJson);
    }
  }

  @Override
  Collection<String> getAttributeNames() throws CloudFormationException {
    Collection<String> copy = Lists.newArrayList(outputAttributes.keySet());
    return copy;
  }
}


@ToString(includeNames=true)
public class AWSCloudFormationWaitConditionResourceInfo extends ResourceInfo {
  @AttributeJson
  String data;
  public AWSCloudFormationWaitConditionResourceInfo() {
    setType("AWS::CloudFormation::WaitCondition");
  }
}


@ToString(includeNames=true)
public class AWSCloudFormationWaitConditionHandleResourceInfo extends ResourceInfo {
  @AttributeJson
  String eucaParts;
  public AWSCloudFormationWaitConditionHandleResourceInfo() {
    setType("AWS::CloudFormation::WaitConditionHandle");
  }
}

@ToString(includeNames=true)
public class AWSCloudWatchAlarmResourceInfo extends ResourceInfo {
  public AWSCloudWatchAlarmResourceInfo() {
    setType("AWS::CloudWatch::Alarm");
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
  @AttributeJson
  String allocationId;
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
  @AttributeJson
  String primaryPrivateIpAddress
  @AttributeJson
  String secondaryPrivateIpAddresses
  public AWSEC2NetworkInterfaceResourceInfo() {
    setType("AWS::EC2::NetworkInterface");
  }
}


// Can't do this one until we allow more than one network interface on an instance
//@ToString(includeNames=true)
//public class AWSEC2NetworkInterfaceAttachmentResourceInfo extends ResourceInfo {
//  public AWSEC2NetworkInterfaceAttachmentResourceInfo() {
//    setType("AWS::EC2::NetworkInterfaceAttachment");
//  }
//}


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
  @AttributeJson
  String availabilityZone;
  public AWSEC2SubnetResourceInfo() {
    setType("AWS::EC2::Subnet");
  }
}

@ToString(includeNames=true)
public class AWSEC2SubnetNetworkAclAssociationResourceInfo extends ResourceInfo {
  @AttributeJson
  String availabilityZone;
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
public class AWSS3BucketResourceInfo extends ResourceInfo {
  @AttributeJson
  String domainName;
  @AttributeJson
  String websiteURL;
  public AWSS3BucketResourceInfo() {
    setType("AWS::S3::Bucket");
  }
}
