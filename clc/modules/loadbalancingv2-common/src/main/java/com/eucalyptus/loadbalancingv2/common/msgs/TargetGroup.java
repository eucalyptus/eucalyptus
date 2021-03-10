/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegex;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class TargetGroup extends EucalyptusData {

  private Boolean healthCheckEnabled;

  @FieldRange(min = 5, max = 300)
  private Integer healthCheckIntervalSeconds;

  @FieldRange(min = 1, max = 1024)
  private String healthCheckPath;

  private String healthCheckPort;

  @FieldRegex(FieldRegexValue.ENUM_PROTOCOLENUM)
  private String healthCheckProtocol;

  @FieldRange(min = 2, max = 120)
  private Integer healthCheckTimeoutSeconds;

  @FieldRange(min = 2, max = 10)
  private Integer healthyThresholdCount;

  private LoadBalancerArns loadBalancerArns;

  private Matcher matcher;

  @FieldRange(min = 1, max = 65535)
  private Integer port;

  @FieldRegex(FieldRegexValue.ENUM_PROTOCOLENUM)
  private String protocol;

  @FieldRegex(FieldRegexValue.ENUM_PROTOCOLVERSIONENUM)
  private String protocolVersion;

  private String targetGroupArn;

  private String targetGroupName;

  @FieldRegex(FieldRegexValue.ENUM_TARGETTYPEENUM)
  private String targetType;

  @FieldRange(min = 2, max = 10)
  private Integer unhealthyThresholdCount;

  private String vpcId;

  public Boolean getHealthCheckEnabled() {
    return healthCheckEnabled;
  }

  public void setHealthCheckEnabled(final Boolean healthCheckEnabled) {
    this.healthCheckEnabled = healthCheckEnabled;
  }

  public Integer getHealthCheckIntervalSeconds() {
    return healthCheckIntervalSeconds;
  }

  public void setHealthCheckIntervalSeconds(final Integer healthCheckIntervalSeconds) {
    this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
  }

  public String getHealthCheckPath() {
    return healthCheckPath;
  }

  public void setHealthCheckPath(final String healthCheckPath) {
    this.healthCheckPath = healthCheckPath;
  }

  public String getHealthCheckPort() {
    return healthCheckPort;
  }

  public void setHealthCheckPort(final String healthCheckPort) {
    this.healthCheckPort = healthCheckPort;
  }

  public String getHealthCheckProtocol() {
    return healthCheckProtocol;
  }

  public void setHealthCheckProtocol(final String healthCheckProtocol) {
    this.healthCheckProtocol = healthCheckProtocol;
  }

  public Integer getHealthCheckTimeoutSeconds() {
    return healthCheckTimeoutSeconds;
  }

  public void setHealthCheckTimeoutSeconds(final Integer healthCheckTimeoutSeconds) {
    this.healthCheckTimeoutSeconds = healthCheckTimeoutSeconds;
  }

  public Integer getHealthyThresholdCount() {
    return healthyThresholdCount;
  }

  public void setHealthyThresholdCount(final Integer healthyThresholdCount) {
    this.healthyThresholdCount = healthyThresholdCount;
  }

  public LoadBalancerArns getLoadBalancerArns() {
    return loadBalancerArns;
  }

  public void setLoadBalancerArns(final LoadBalancerArns loadBalancerArns) {
    this.loadBalancerArns = loadBalancerArns;
  }

  public Matcher getMatcher() {
    return matcher;
  }

  public void setMatcher(final Matcher matcher) {
    this.matcher = matcher;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(final Integer port) {
    this.port = port;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(final String protocol) {
    this.protocol = protocol;
  }

  public String getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(String protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  public String getTargetGroupArn() {
    return targetGroupArn;
  }

  public void setTargetGroupArn(final String targetGroupArn) {
    this.targetGroupArn = targetGroupArn;
  }

  public String getTargetGroupName() {
    return targetGroupName;
  }

  public void setTargetGroupName(final String targetGroupName) {
    this.targetGroupName = targetGroupName;
  }

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(final String targetType) {
    this.targetType = targetType;
  }

  public Integer getUnhealthyThresholdCount() {
    return unhealthyThresholdCount;
  }

  public void setUnhealthyThresholdCount(final Integer unhealthyThresholdCount) {
    this.unhealthyThresholdCount = unhealthyThresholdCount;
  }

  public String getVpcId() {
    return vpcId;
  }

  public void setVpcId(final String vpcId) {
    this.vpcId = vpcId;
  }

}
