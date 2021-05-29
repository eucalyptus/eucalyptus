/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import java.util.ArrayList;

public class AWSElasticLoadBalancingV2TargetGroupProperties implements ResourceProperties {


  @Property
  private String name;

  @Property
  private Integer port;

  @Property
  private String protocol;

  @Property
  private String protocolVersion;

  @Property
  private Boolean healthCheckEnabled;

  @Property
  private Integer healthCheckIntervalSeconds;

  @Property
  private String healthCheckPath;

  @Property
  private String healthCheckPort;

  @Property
  private String healthCheckProtocol;

  @Property
  private Integer healthCheckTimeoutSeconds;

  @Property
  private Integer healthyThresholdCount;

  @Property
  private ElasticLoadBalancingV2MatcherProperties matcher;

  @Property
  private String targetType;

  @Property
  private ArrayList<ElasticLoadBalancingV2TargetDescriptionProperties> targets = Lists.newArrayList( );

  @Property
  private Integer unhealthyThresholdCount;

  @Property
  private String vpcId;

  @Property
  private ArrayList<ElasticLoadBalancingV2TargetGroupAttributeProperties> targetGroupAttributes =
      Lists.newArrayList();

  @Property
  private ArrayList<CloudFormationResourceTag> tags = Lists.newArrayList( );

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(String protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  public Boolean getHealthCheckEnabled() {
    return healthCheckEnabled;
  }

  public void setHealthCheckEnabled(Boolean healthCheckEnabled) {
    this.healthCheckEnabled = healthCheckEnabled;
  }

  public Integer getHealthCheckIntervalSeconds() {
    return healthCheckIntervalSeconds;
  }

  public void setHealthCheckIntervalSeconds(Integer healthCheckIntervalSeconds) {
    this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
  }

  public String getHealthCheckPath() {
    return healthCheckPath;
  }

  public void setHealthCheckPath(String healthCheckPath) {
    this.healthCheckPath = healthCheckPath;
  }

  public String getHealthCheckPort() {
    return healthCheckPort;
  }

  public void setHealthCheckPort(String healthCheckPort) {
    this.healthCheckPort = healthCheckPort;
  }

  public String getHealthCheckProtocol() {
    return healthCheckProtocol;
  }

  public void setHealthCheckProtocol(String healthCheckProtocol) {
    this.healthCheckProtocol = healthCheckProtocol;
  }

  public Integer getHealthCheckTimeoutSeconds() {
    return healthCheckTimeoutSeconds;
  }

  public void setHealthCheckTimeoutSeconds(Integer healthCheckTimeoutSeconds) {
    this.healthCheckTimeoutSeconds = healthCheckTimeoutSeconds;
  }

  public Integer getHealthyThresholdCount() {
    return healthyThresholdCount;
  }

  public void setHealthyThresholdCount(Integer healthyThresholdCount) {
    this.healthyThresholdCount = healthyThresholdCount;
  }

  public ElasticLoadBalancingV2MatcherProperties getMatcher() {
    return matcher;
  }

  public void setMatcher(
      ElasticLoadBalancingV2MatcherProperties matcher) {
    this.matcher = matcher;
  }

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public ArrayList<ElasticLoadBalancingV2TargetDescriptionProperties> getTargets() {
    return targets;
  }

  public void setTargets(
      ArrayList<ElasticLoadBalancingV2TargetDescriptionProperties> targets) {
    this.targets = targets;
  }

  public Integer getUnhealthyThresholdCount() {
    return unhealthyThresholdCount;
  }

  public void setUnhealthyThresholdCount(Integer unhealthyThresholdCount) {
    this.unhealthyThresholdCount = unhealthyThresholdCount;
  }

  public String getVpcId() {
    return vpcId;
  }

  public void setVpcId(String vpcId) {
    this.vpcId = vpcId;
  }

  public ArrayList<ElasticLoadBalancingV2TargetGroupAttributeProperties> getTargetGroupAttributes() {
    return targetGroupAttributes;
  }

  public void setTargetGroupAttributes(
      ArrayList<ElasticLoadBalancingV2TargetGroupAttributeProperties> targetGroupAttributes) {
    this.targetGroupAttributes = targetGroupAttributes;
  }

  public ArrayList<CloudFormationResourceTag> getTags() {
    return tags;
  }

  public void setTags(
      ArrayList<CloudFormationResourceTag> tags) {
    this.tags = tags;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("port", port)
        .add("protocol", protocol)
        .add("protocolVersion", protocolVersion)
        .add("healthCheckEnabled", healthCheckEnabled)
        .add("healthCheckIntervalSeconds", healthCheckIntervalSeconds)
        .add("healthCheckPath", healthCheckPath)
        .add("healthCheckPort", healthCheckPort)
        .add("healthCheckProtocol", healthCheckProtocol)
        .add("healthCheckTimeoutSeconds", healthCheckTimeoutSeconds)
        .add("healthyThresholdCount", healthyThresholdCount)
        .add("matcher", matcher)
        .add("targetType", targetType)
        .add("targets", targets)
        .add("unhealthyThresholdCount", unhealthyThresholdCount)
        .add("vpcId", vpcId)
        .add("targetGroupAttributes", targetGroupAttributes)
        .add("tags", tags)
        .toString();
  }
}
