/*************************************************************************
 * Copyright 2013-2015 Ent. Services Development Corporation LP
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
  @AttributeJson
  String eucaCreateStartTime; // used to check against timeout

  public AWSAutoScalingAutoScalingGroupResourceInfo() {
    setType("AWS::AutoScaling::AutoScalingGroup");
  }
  @Override
  public boolean supportsSignals() {
    return true;
  }
  @Override
  public boolean supportsTags() {
    return true;
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

  Map<String, String> outputAttributes = Maps.newLinkedHashMap();
  Map<String, String> eucaAttributes = Maps.newLinkedHashMap();

  @Override
  boolean isAttributeAllowed(String attributeName) {
    return attributeName != null && (attributeName.startsWith("Euca.") || attributeName.startsWith("Outputs."));
  }

  @Override
  String getResourceAttributeJson(String attributeName) throws CloudFormationException {
    if (outputAttributes.containsKey(attributeName)) {
      return outputAttributes.get(attributeName);
    } else if (eucaAttributes.containsKey(attributeName)) {
      return eucaAttributes.get(attributeName);
    } else {
      throw new ValidationErrorException("Stack does not have an attribute named " + attributeName);
    }
  }

  @Override
  void setResourceAttributeJson(String attributeName, String attributeValueJson) throws CloudFormationException {
    if (attributeName == null || (!attributeName.startsWith("Outputs.") && !attributeName.startsWith("Euca."))) {
      throw new ValidationErrorException("Stack can not have an attribute named " + attributeName);
    } else if (attributeName.startsWith("Outputs.")) {
      outputAttributes.put(attributeName, attributeValueJson);
    } else if (attributeName.startsWith("Euca.")) {
      eucaAttributes.put(attributeName, attributeValueJson)
    }
  }

  @Override
  Collection<String> getAttributeNames() throws CloudFormationException {
    Collection<String> copy = Lists.newArrayList(outputAttributes.keySet());
    copy.addAll(eucaAttributes.keySet());
    return copy;
  }
  @Override
  public Collection<String> getRequiredCapabilities() {
    ArrayList<String> capabilities = new ArrayList<String>();
    capabilities.add("CAPABILITY_IAM");
    return capabilities;
  }

  public final static String EUCA_DELETE_STATUS_UPDATE_COMPLETE_CLEANUP_IN_PROGRESS = "Euca.DeleteStatusUpdateCompleteCleanupInProgress";
  public final static String EUCA_DELETE_STATUS_UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS = "Euca.DeleteStatusUpdateCompleteCleanupInProgress";
}


@ToString(includeNames=true)
public class AWSCloudFormationWaitConditionResourceInfo extends ResourceInfo {
  @AttributeJson
  String data;
  @AttributeJson
  String eucaCreateStartTime; // used to check against timeout
  public AWSCloudFormationWaitConditionResourceInfo() {
    setType("AWS::CloudFormation::WaitCondition");
  }
  @Override
  public boolean supportsSignals() {
    return true;
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
  @Override
  public boolean supportsTags() {
    return true;
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
  @AttributeJson
  String eucaCreateStartTime; // used to check against timeout

  public AWSEC2InstanceResourceInfo() {
    setType("AWS::EC2::Instance");
  }
  @Override
  public boolean supportsSignals() {
    return true;
  }
  @Override
  public boolean supportsTags() {
    return true;
  }

}


@ToString(includeNames=true)
public class AWSEC2InternetGatewayResourceInfo extends ResourceInfo {
  public AWSEC2InternetGatewayResourceInfo() {
    setType("AWS::EC2::InternetGateway");
  }
  @Override
  public boolean supportsTags() {
    return true;
  }
}


@ToString(includeNames=true)
public class AWSEC2NatGatewayResourceInfo extends ResourceInfo {
  public AWSEC2NatGatewayResourceInfo() {
    setType("AWS::EC2::NatGateway");
  }
}

@ToString(includeNames=true)
public class AWSEC2NetworkAclResourceInfo extends ResourceInfo {
  public AWSEC2NetworkAclResourceInfo() {
    setType("AWS::EC2::NetworkAcl");
  }
  @Override
  public boolean supportsTags() {
    return true;
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
  @Override
  public boolean supportsTags() {
    return true;
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
  @Override
  public boolean supportsTags() {
    return true;
  }
}


@ToString(includeNames=true)
public class AWSEC2SecurityGroupResourceInfo extends ResourceInfo {
  @AttributeJson
  String groupId;
  public AWSEC2SecurityGroupResourceInfo() {
    setType("AWS::EC2::SecurityGroup");
  }
  @Override
  public boolean supportsTags() {
    return true;
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
  @Override
  public boolean supportsTags() {
    return true;
  }
}

@ToString(includeNames=true)
public class AWSEC2SubnetNetworkAclAssociationResourceInfo extends ResourceInfo {
  @AttributeJson
  String associationId;

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

  @AttributeJson
  String snapshotIdForDelete;
  public AWSEC2VolumeResourceInfo() {
    setType("AWS::EC2::Volume");
  }
  @Override
  public boolean supportsSnapshot() {
    return true;
  }
  @Override
  public boolean supportsTags() {
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
  @AttributeJson
  String cidrBlock;
  @AttributeJson
  String defaultNetworkAcl;
  @AttributeJson
  String defaultSecurityGroup;
  public AWSEC2VPCResourceInfo() {
    setType("AWS::EC2::VPC");
  }
  @Override
  public boolean supportsTags() {
    return true;
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
  @Override
  public boolean supportsTags() {
    return true;
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
  @Override
  public boolean supportsTags() {
    return true;
  }
}
