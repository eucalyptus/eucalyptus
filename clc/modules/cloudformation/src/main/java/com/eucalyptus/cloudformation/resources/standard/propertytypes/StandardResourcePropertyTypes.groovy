/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.cloudformation.resources.standard.propertytypes

import com.eucalyptus.cloudformation.resources.annotations.Property
import com.eucalyptus.cloudformation.resources.annotations.Required
import com.fasterxml.jackson.databind.JsonNode
import com.google.common.collect.Lists
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

// Note: all of these items must properly override equals & hashcode for equality for update to detect differences
@EqualsAndHashCode
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

@EqualsAndHashCode
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

@EqualsAndHashCode
@ToString(includeNames=true)
public class AutoScalingNotificationConfiguration {
  @Property
  @Required
  String topicARN;
  @Property
  @Required
  List<String> notificationTypes = Lists.newArrayList();
}

@EqualsAndHashCode
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
@EqualsAndHashCode
@ToString(includeNames=true)
public class CloudWatchMetricDimension {
  @Property
  @Required
  String name;
  @Property
  @Required
  String value;
}

@EqualsAndHashCode
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

@EqualsAndHashCode
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
@EqualsAndHashCode
public class EC2ICMP {
  @Property
  Integer code;
  @Property
  Integer type;
}
@EqualsAndHashCode
@ToString(includeNames=true)
public class EC2MountPoint {
  @Required
  @Property
  String device;
  @Required
  @Property
  String volumeId;
}
@EqualsAndHashCode
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
@EqualsAndHashCode
@ToString(includeNames=true)
public class EC2NetworkInterfacePrivateIPSpecification {
  @Required
  @Property
  String privateIpAddress;
  @Required
  @Property
  Boolean primary;
}

@EqualsAndHashCode
public class EC2PortRange {
  @Property
  Integer from;
  @Property
  Integer to;
}

@EqualsAndHashCode
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

@EqualsAndHashCode
@ToString(includeNames=true)
public class EC2Tag {
  @Property
  String key;
  @Property
  String value;
}
@EqualsAndHashCode
@ToString(includeNames=true)
public class EmbeddedIAMPolicy {
  @Required
  @Property
  String policyName;
  @Required
  @Property
  JsonNode policyDocument;
}

@EqualsAndHashCode
@ToString(includeNames=true)
public class LoginProfile {
  @Property
  String password;
}

@EqualsAndHashCode
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

@EqualsAndHashCode
@ToString(includeNames=true)
public class ElasticLoadBalancingAppCookieStickinessPolicy {
  @Required
  @Property
  String cookieName;
  @Required
  @Property
  String policyName;
}

@EqualsAndHashCode
@ToString(includeNames=true)
public class ElasticLoadBalancingConnectionDrainingPolicy {
  @Required
  @Property
  Boolean enabled;
  @Property
  Integer timeout;
}

@EqualsAndHashCode
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

@EqualsAndHashCode
@ToString(includeNames=true)
public class ElasticLoadBalancingLBCookieStickinessPolicyType {
  @Property
  Long cookieExpirationPeriod;
  @Required
  @Property
  String policyName;
}

@EqualsAndHashCode
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

@EqualsAndHashCode
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


@EqualsAndHashCode
@ToString(includeNames=true)
public class ElasticLoadBalancingPolicyTypeAttribute {
  @Required
  @Property
  String name;
  @Required
  @Property
  String value;
}

@EqualsAndHashCode
@ToString(includeNames=true)
public class S3CorsConfiguration {
  @Required
  @Property
  List<S3CorsConfigurationRule> corsRules = Lists.newArrayList();
}

@EqualsAndHashCode
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

@EqualsAndHashCode
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

@EqualsAndHashCode
@ToString(includeNames=true)
public class S3LifecycleConfiguration {
  @Required
  @Property
  List<S3LifecycleRule> rules = Lists.newArrayList();
}

@EqualsAndHashCode
@ToString(includeNames=true)
public class S3LoggingConfiguration {
  @Property
  String destinationBucketName;
  @Property
  String logFilePrefix;
}

@EqualsAndHashCode
@ToString(includeNames=true)
public class S3NotificationConfiguration{
  @Required
  @Property
  List<S3NotificationTopicConfiguration> topicConfigurations = Lists.newArrayList();
}

@EqualsAndHashCode
@ToString(includeNames=true)
public class S3ReplicationConfiguration{
  @Required
  @Property
  String role;
  @Required
  @Property
  List<S3ReplicationConfigurationRule> rules = Lists.newArrayList();
}

@EqualsAndHashCode
@ToString(includeNames=true)
public class S3ReplicationConfigurationRule {
  @Required
  @Property
  S3ReplicationConfigurationRulesDestination destination;
  @Property
  String id;
  @Required
  @Property
  String prefix;
  @Required
  @Property
  String status;
}

@EqualsAndHashCode
@ToString(includeNames=true)
public class S3ReplicationConfigurationRulesDestination {
  @Required
  @Property
  String bucket;
  @Property
  String storageClass;
}

@EqualsAndHashCode
@ToString(includeNames=true)
public class CloudFormationResourceTag {
  @Required
  @Property
  String key;
  @Required
  @Property
  String value;

}

@EqualsAndHashCode
@ToString(includeNames=true)
public class PrivateIpAddressSpecification {
  @Required
  @Property
  String privateIpAddress;
  @Required
  @Property
  Boolean primary;
}

@EqualsAndHashCode
@ToString(includeNames=true)
public class S3VersioningConfiguration {
  @Required
  @Property
  String status;
}

@EqualsAndHashCode
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

@EqualsAndHashCode
@ToString(includeNames=true)
public class S3WebsiteConfigurationRoutingRule {
  @Required
  @Property
  S3WebsiteConfigurationRoutingRulesRedirectRule redirectRule;
  @Property
  S3WebsiteConfigurationRoutingRulesRoutingRuleCondition routingRuleCondition;
}

@EqualsAndHashCode
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

@EqualsAndHashCode
@ToString(includeNames=true)
public class S3WebsiteConfigurationRoutingRulesRoutingRuleCondition {
  @Property
  String httpErrorCodeReturnedEquals;
  @Property
  String keyPrefixEquals;
}

@EqualsAndHashCode
@ToString(includeNames=true)
public class S3WebsiteConfigurationRedirectAllRequestsTo {
  @Required
  @Property
  String hostName;
  @Property
  String protocol;
}

@EqualsAndHashCode
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

@EqualsAndHashCode
@ToString(includeNames=true)
public class S3NotificationTopicConfiguration {
  @Required
  @Property
  String event;
  @Required
  @Property
  String topic;
}







