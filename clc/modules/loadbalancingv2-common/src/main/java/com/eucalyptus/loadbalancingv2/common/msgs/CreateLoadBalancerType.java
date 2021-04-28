/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegex;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegexValue;


public class CreateLoadBalancerType extends Loadbalancingv2Message {

  @FieldRegex(FieldRegexValue.ENUM_IPADDRESSTYPE)
  private String ipAddressType;

  @Nonnull
  private String name;

  @FieldRegex(FieldRegexValue.ENUM_LOADBALANCERSCHEMEENUM)
  private String scheme;

  private SecurityGroups securityGroups;

  private SubnetMappings subnetMappings;

  private Subnets subnets;

  @FieldRange(min = 1)
  private TagList tags;

  @FieldRegex(FieldRegexValue.ENUM_LOADBALANCERTYPEENUM)
  private String type;

  public String getIpAddressType() {
    return ipAddressType;
  }

  public void setIpAddressType(final String ipAddressType) {
    this.ipAddressType = ipAddressType;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
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

  public SubnetMappings getSubnetMappings() {
    return subnetMappings;
  }

  public void setSubnetMappings(final SubnetMappings subnetMappings) {
    this.subnetMappings = subnetMappings;
  }

  public Subnets getSubnets() {
    return subnets;
  }

  public void setSubnets(final Subnets subnets) {
    this.subnets = subnets;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

}
