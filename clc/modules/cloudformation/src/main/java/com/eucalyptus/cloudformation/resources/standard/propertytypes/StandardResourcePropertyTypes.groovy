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

import com.eucalyptus.cloudformation.resources.annotations.Property
import com.eucalyptus.cloudformation.resources.annotations.Required
import com.fasterxml.jackson.databind.JsonNode
import com.google.common.collect.Lists
import groovy.transform.ToString

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
public class EC2ICMP {
  @Property
  Integer code;
  @Property
  Integer type;
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
  Integer deviceIndex;
  @Property
  List<String> groupSet = Lists.newArrayList();
  @Property
  String networkInterfaceId;
  @Property
  String privateIpAddress;
  @Property
  List<EC2NetworkInterfacePrivateIPSpecification> privateIpAddresses = Lists.newArrayList();
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

public class EC2PortRange {
  @Property
  Integer from;
  @Property
  Integer to;
}

@ToString(includeNames=true)
public class EC2SecurityGroupRule {
  @Property
  String cidrIp;
  @Property
  String destinationSecurityGroupId;
  @Property
  Integer fromPort;
  @Required
  @Property
  String ipProtocol;
  @Property
  String sourceSecurityGroupId;
  @Property
  String sourceSecurityGroupName;
  @Property
  String sourceSecurityGroupOwnerId;
  @Property
  Integer toPort;
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
public class ElasticLoadBalancingConnectionSettings {
  @Required
  @Property
  Integer idleTimeout;
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
  List<String> policyNames = Lists.newArrayList();
  @Required
  @Property
  String protocol;
  @Property(name="SSLCertificateId")
  String sslCertificateId;
}

@ToString(includeNames=true)
public class ElasticLoadBalancingPolicyType {
  @Property
  List<ElasticLoadBalancingPolicyTypeAttribute> attributes = Lists.newArrayList();
  @Property
  List<Integer> instancePorts = Lists.newArrayList();
  @Property
  List<Integer> loadBalancerPorts = Lists.newArrayList();
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

@ToString(includeNames=true)
public class S3CorsConfiguration {
  @Required
  @Property
  List<S3CorsConfigurationRule> corsRule = Lists.newArrayList();
}

@ToString(includeNames=true)
public class S3CorsConfigurationRule {
  @Property
  List<String> allowedHeaders = Lists.newArrayList();
  @Required
  @Property
  List<String> allowedMethods = Lists.newArrayList();
  @Required
  @Property
  List<String> allowedOrigins = Lists.newArrayList();
  @Property
  List<String> exposedHeaders = Lists.newArrayList();
  @Property
  String id;
  @Property
  Integer maxAge;
}
@ToString(includeNames=true)
public class S3LifecycleRule {
  @Property
  String expirationDate;
  @Property
  Integer expirationInDays;
  @Property
  String id;
  @Property
  String prefix;
  @Required
  @Property
  String status;
  @Property
  S3LifecycleRuleTransition transition;
}
@ToString(includeNames=true)
public class S3LifecycleConfiguration {
  @Required
  @Property
  List<S3LifecycleRule> rules = Lists.newArrayList();
}

@ToString(includeNames=true)
public class S3LoggingConfiguration {
  @Property
  String destinationBucketName;
  @Property
  String logFilePrefix;
}

@ToString(includeNames=true)
public class S3NotificationConfiguration{
  @Required
  @Property
  List<S3NotificationTopicConfiguration> topicConfigurations = Lists.newArrayList();
}
@ToString(includeNames=true)
public class CloudFormationResourceTag {
  @Required
  @Property
  String key;
  @Required
  @Property
  String value;

}
@ToString(includeNames=true)
public class PrivateIpAddressSpecification {
  @Required
  @Property
  String privateIpAddress;
  @Required
  @Property
  Boolean primary;
}

@ToString(includeNames=true)
public class S3VersioningConfiguration {
  @Required
  @Property
  String status;
}
@ToString(includeNames=true)
public class S3WebsiteConfiguration {
  @Property
  String errorDocument;
  @Property
  String indexDocument;
  @Property
  S3WebsiteConfigurationRedirectAllRequestsTo redirectAllRequestsTo;
  @Property
  List<S3WebsiteConfigurationRoutingRule> routingRules = Lists.newArrayList();
}

@ToString(includeNames=true)
public class S3WebsiteConfigurationRoutingRule {
  @Required
  @Property
  S3WebsiteConfigurationRoutingRulesRedirectRule redirectRule;
  @Property
  S3WebsiteConfigurationRoutingRulesRoutingRuleCondition routingRuleCondition;
}

@ToString(includeNames=true)
public class S3WebsiteConfigurationRoutingRulesRedirectRule {
  @Property
  String hostName;
  @Property
  String httpRedirectCode;
  @Property
  String protocol;
  @Property
  String replaceKeyPrefixWith;
  @Property
  String replaceKeyWith;
}

@ToString(includeNames=true)
public class S3WebsiteConfigurationRoutingRulesRoutingRuleCondition {
  @Property
  String httpErrorCodeReturnedEquals;
  @Property
  String keyPrefixEquals;
}

@ToString(includeNames=true)
public class S3WebsiteConfigurationRedirectAllRequestsTo {
  @Required
  @Property
  String hostName;
  @Property
  String protocol;
}

@ToString(includeNames=true)
public class S3LifecycleRuleTransition {
  @Required
  @Property
  String storageClass;
  @Property
  String transitionDate;
  @Property
  Integer transitionInDays;
}

@ToString(includeNames=true)
public class S3NotificationTopicConfiguration {
  @Required
  @Property
  String event;
  @Required
  @Property
  String topic;
}







