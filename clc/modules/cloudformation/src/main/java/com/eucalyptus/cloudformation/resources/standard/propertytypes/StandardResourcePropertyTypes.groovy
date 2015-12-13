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
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

// Note: all of these items must properly override equals & hashcode for equality for update to detect differences
@EqualsAndHashCode(includeFields=true)
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

@EqualsAndHashCode(includeFields=true)
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

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class AutoScalingNotificationConfiguration {
  @Property
  @Required
  String topicARN;
  @Property
  @Required
  List<String> notificationTypes = Lists.newArrayList();
}

@EqualsAndHashCode(includeFields=true)
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
@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class CloudWatchMetricDimension {
  @Property
  @Required
  String name;
  @Property
  @Required
  String value;
}

@EqualsAndHashCode(includeFields=true)
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

@EqualsAndHashCode(includeFields=true)
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
@EqualsAndHashCode(includeFields=true)
public class EC2ICMP {
  @Property
  Integer code;
  @Property
  Integer type;
}
@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class EC2MountPoint {
  @Required
  @Property
  String device;
  @Required
  @Property
  String volumeId;
}
@EqualsAndHashCode(includeFields=true)
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
@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class EC2NetworkInterfacePrivateIPSpecification {
  @Required
  @Property
  String privateIpAddress;
  @Required
  @Property
  Boolean primary;
}

@EqualsAndHashCode(includeFields=true)
public class EC2PortRange {
  @Property
  Integer from;
  @Property
  Integer to;
}

@EqualsAndHashCode(includeFields=true)
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

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class EC2Tag {
  @Property
  String key;
  @Property
  String value;
}
@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class EmbeddedIAMPolicy {
  @Required
  @Property
  String policyName;
  @Required
  @Property
  JsonNode policyDocument;
}

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class LoginProfile {
  @Property
  String password;
}

@EqualsAndHashCode(includeFields=true)
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

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class ElasticLoadBalancingAppCookieStickinessPolicy {
  @Required
  @Property
  String cookieName;
  @Required
  @Property
  String policyName;
}

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class ElasticLoadBalancingConnectionDrainingPolicy {
  @Required
  @Property
  Boolean enabled;
  @Property
  Integer timeout;
}

@EqualsAndHashCode(includeFields=true)
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

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class ElasticLoadBalancingLBCookieStickinessPolicyType {
  @Property
  Long cookieExpirationPeriod;
  @Required
  @Property
  String policyName;
}

@EqualsAndHashCode(includeFields=true)
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

@EqualsAndHashCode(includeFields=true)
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


@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class ElasticLoadBalancingPolicyTypeAttribute {
  @Required
  @Property
  String name;
  @Required
  @Property
  String value;
}

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class S3CorsConfiguration {
  @Required
  @Property
  List<S3CorsConfigurationRule> corsRule = Lists.newArrayList();
}

@EqualsAndHashCode(includeFields=true)
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

@EqualsAndHashCode(includeFields=true)
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

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class S3LifecycleConfiguration {
  @Required
  @Property
  List<S3LifecycleRule> rules = Lists.newArrayList();
}

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class S3LoggingConfiguration {
  @Property
  String destinationBucketName;
  @Property
  String logFilePrefix;
}

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class S3NotificationConfiguration{
  @Required
  @Property
  List<S3NotificationTopicConfiguration> topicConfigurations = Lists.newArrayList();
}

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class CloudFormationResourceTag {
  @Required
  @Property
  String key;
  @Required
  @Property
  String value;

}

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class PrivateIpAddressSpecification {
  @Required
  @Property
  String privateIpAddress;
  @Required
  @Property
  Boolean primary;
}

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class S3VersioningConfiguration {
  @Required
  @Property
  String status;
}

@EqualsAndHashCode(includeFields=true)
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

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class S3WebsiteConfigurationRoutingRule {
  @Required
  @Property
  S3WebsiteConfigurationRoutingRulesRedirectRule redirectRule;
  @Property
  S3WebsiteConfigurationRoutingRulesRoutingRuleCondition routingRuleCondition;
}

@EqualsAndHashCode(includeFields=true)
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

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class S3WebsiteConfigurationRoutingRulesRoutingRuleCondition {
  @Property
  String httpErrorCodeReturnedEquals;
  @Property
  String keyPrefixEquals;
}

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class S3WebsiteConfigurationRedirectAllRequestsTo {
  @Required
  @Property
  String hostName;
  @Property
  String protocol;
}

@EqualsAndHashCode(includeFields=true)
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

@EqualsAndHashCode(includeFields=true)
@ToString(includeNames=true)
public class S3NotificationTopicConfiguration {
  @Required
  @Property
  String event;
  @Required
  @Property
  String topic;
}







