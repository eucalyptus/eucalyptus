package com.eucalyptus.cloudformation.resources.standard.propertytypes

import com.eucalyptus.cloudformation.resources.annotations.Property
import com.eucalyptus.cloudformation.resources.annotations.Required
import com.fasterxml.jackson.databind.JsonNode
import com.google.common.collect.Lists
import groovy.transform.ToString

/**
 * Created by ethomas on 2/2/14.
 */

@ToString(includeNames=true)
public class AutoScalingBlockDeviceMapping {
  @Property
  @Required
  String deviceName;
  @Property
  AutoScalingEBSBlockDevice ebs;
  @Property
  Boolean noDevice;
  @Property
  String virtualName;
}

@ToString(includeNames=true)
public class AutoScalingEBSBlockDevice {
  @Property
  Boolean deleteOnTermination;
  @Property
  Integer iops;
  @Property
  String snapshotId;
  @Property
  Integer volumeSize;
  @Property
  String volumeType;
}

@ToString(includeNames=true)
public class AutoScalingNotificationConfiguration {
  @Property
  @Required
  String topicARN;
  @Property
  @Required
  List<String> notificationTypes = Lists.newArrayList();
}

@ToString(includeNames=true)
public class AutoScalingTag {
  @Property
  @Required
  String key;
  @Property
  @Required
  String value;
  @Property
  @Required
  Boolean propagateAtLaunch;
}
@ToString(includeNames=true)
public class CloudWatchMetricDimension {
  @Property
  @Required
  String name;
  @Property
  @Required
  String value;
}

@ToString(includeNames=true)
public class EC2BlockDeviceMapping {
  @Property
  @Required
  String deviceName;
  @Property
  EC2EBSBlockDevice ebs;
  @Property
  Object noDevice;
  @Property
  String virtualName;
}

@ToString(includeNames=true)
public class EC2EBSBlockDevice {
  @Property
  Boolean deleteOnTermination;
  @Property
  Integer iops;
  @Property
  String snapshotId;
  @Property
  String volumeSize;
  @Property
  String volumeType;
}
@ToString(includeNames=true)
public class EC2MountPoint {
  @Required
  @Property
  String device;
  @Required
  @Property
  String volumeId;
}
@ToString(includeNames=true)
public class EC2NetworkInterface {
  @Property
  Boolean associatePublicIpAddress;
  @Property
  Boolean deleteOnTermination;
  @Property
  String description;
  @Required
  @Property
  List<String> deviceIndex;
  @Property
  List<String> groupSet;
  @Property
  String networkInterfaceId;
  @Property
  String privateIpAddress;
  @Property
  List<EC2NetworkInterfacePrivateIPSpecification> privateIpAddresses;
  @Property
  Integer secondaryPrivateIpAddressCount;
  @Property
  String subnetId;
}
@ToString(includeNames=true)
public class EC2NetworkInterfacePrivateIPSpecification {
  @Required
  @Property
  String privateIpAddress;
  @Required
  @Property
  Boolean primary;
}
@ToString(includeNames=true)
public class EC2SecurityGroupRule {
  @Property
  String cidrIp;
  @Property
  String destinationSecurityGroupId;
  @Required
  @Property
  String fromPort;
  @Required
  @Property
  String ipProtocol;
  @Property
  String sourceSecurityGroupId;
  @Property
  String sourceSecurityGroupName;
  @Property
  String sourceSecurityGroupOwnerId;
  @Required
  @Property
  String toPort;
}
@ToString(includeNames=true)
public class EC2Tag {
  @Property
  String key;
  @Property
  String value;
}
@ToString(includeNames=true)
public class EmbeddedIAMPolicy {
  @Required
  @Property
  String policyName;
  @Required
  @Property
  JsonNode policyDocument;
}

@ToString(includeNames=true)
public class LoginProfile {
  @Property
  String password;
}

@ToString(includeNames=true)
public class ElasticLoadBalancingAccessLoggingPolicy {
  @Property
  Integer emitInterval;
  @Required
  @Property
  Boolean enabled;
  @Property(name="S3BucketName")
  String s3BucketName;
  @Property(name="S3BucketPrefix")
  String s3BucketPrefix;
}

@ToString(includeNames=true)
public class ElasticLoadBalancingAppCookieStickinessPolicy {
  @Required
  @Property
  String cookieName;
  @Required
  @Property
  String policyName;
}

@ToString(includeNames=true)
public class ElasticLoadBalancingConnectionDrainingPolicy {
  @Required
  @Property
  Boolean enabled;
  @Property
  Integer timeout;
}

@ToString(includeNames=true)
public class ElasticLoadBalancingHealthCheckType {
  @Property
  Integer healthyThreshold;
  @Property
  Integer interval;
  @Property
  String target;
  @Property
  Integer timeout;
  @Property
  Integer unhealthyThreshold;
}

@ToString(includeNames=true)
public class ElasticLoadBalancingLBCookieStickinessPolicyType {
  @Property
  Long cookieExpirationPeriod;
  @Required
  @Property
  String policyName;
}

@ToString(includeNames=true)
public class ElasticLoadBalancingListener {
  @Required
  @Property
  Integer instancePort;
  @Property
  String instanceProtocol;
  @Required
  @Property
  Integer loadBalancerPort;
  @Property
  List<String> policyNames;
  @Required
  @Property
  String protocol;
  @Property(name="SSLCertificateId")
  String sslCertificateId;
}

@ToString(includeNames=true)
public class ElasticLoadBalancingPolicyType {
  List<ElasticLoadBalancingPolicyTypeAttribute> attributes = Lists.newArrayList();
  @Property
  List<String> instancePorts = Lists.newArrayList();
  @Property
  List<String> loadBalancerPorts = Lists.newArrayList();
  @Required
  @Property
  String policyName;
  @Required
  @Property
  String policyType;
}


@ToString(includeNames=true)
public class ElasticLoadBalancingPolicyTypeAttribute {
  @Required
  @Property
  String name;
  @Required
  @Property
  String value;
}





