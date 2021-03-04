/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;


public class SetSubnetsType extends Loadbalancingv2Message {

  @Nonnull
  private String loadBalancerArn;

  private SubnetMappings subnetMappings;

  private Subnets subnets;

  public String getLoadBalancerArn() {
    return loadBalancerArn;
  }

  public void setLoadBalancerArn(final String loadBalancerArn) {
    this.loadBalancerArn = loadBalancerArn;
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

}
