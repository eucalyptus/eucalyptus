/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegex;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LoadBalancer extends EucalyptusData {

  private AvailabilityZones availabilityZones;

  private String canonicalHostedZoneId;

  private java.util.Date createdTime;

  private String dNSName;

  @FieldRegex(FieldRegexValue.ENUM_IPADDRESSTYPE)
  private String ipAddressType;

  private String loadBalancerArn;

  private String loadBalancerName;

  @FieldRegex(FieldRegexValue.ENUM_LOADBALANCERSCHEMEENUM)
  private String scheme;

  private SecurityGroups securityGroups;

  private LoadBalancerState state;

  @FieldRegex(FieldRegexValue.ENUM_LOADBALANCERTYPEENUM)
  private String type;

  private String vpcId;

  public AvailabilityZones getAvailabilityZones() {
    return availabilityZones;
  }

  public void setAvailabilityZones(final AvailabilityZones availabilityZones) {
    this.availabilityZones = availabilityZones;
  }

  public String getCanonicalHostedZoneId() {
    return canonicalHostedZoneId;
  }

  public void setCanonicalHostedZoneId(final String canonicalHostedZoneId) {
    this.canonicalHostedZoneId = canonicalHostedZoneId;
  }

  public java.util.Date getCreatedTime() {
    return createdTime;
  }

  public void setCreatedTime(final java.util.Date createdTime) {
    this.createdTime = createdTime;
  }

  public String getDNSName() {
    return dNSName;
  }

  public void setDNSName(final String dNSName) {
    this.dNSName = dNSName;
  }

  public String getIpAddressType() {
    return ipAddressType;
  }

  public void setIpAddressType(final String ipAddressType) {
    this.ipAddressType = ipAddressType;
  }

  public String getLoadBalancerArn() {
    return loadBalancerArn;
  }

  public void setLoadBalancerArn(final String loadBalancerArn) {
    this.loadBalancerArn = loadBalancerArn;
  }

  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  public void setLoadBalancerName(final String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  public String getScheme() {
    return scheme;
  }

  public void setScheme(final String scheme) {
    this.scheme = scheme;
  }

  public SecurityGroups getSecurityGroups() {
    return securityGroups;
  }

  public void setSecurityGroups(final SecurityGroups securityGroups) {
    this.securityGroups = securityGroups;
  }

  public LoadBalancerState getState() {
    return state;
  }

  public void setState(final LoadBalancerState state) {
    this.state = state;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getVpcId() {
    return vpcId;
  }

  public void setVpcId(final String vpcId) {
    this.vpcId = vpcId;
  }

}
