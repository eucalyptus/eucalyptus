/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;


public class SetSecurityGroupsType extends Loadbalancingv2Message {

  @Nonnull
  private String loadBalancerArn;

  @Nonnull
  private SecurityGroups securityGroups;

  public String getLoadBalancerArn() {
    return loadBalancerArn;
  }

  public void setLoadBalancerArn(final String loadBalancerArn) {
    this.loadBalancerArn = loadBalancerArn;
  }

  public SecurityGroups getSecurityGroups() {
    return securityGroups;
  }

  public void setSecurityGroups(final SecurityGroups securityGroups) {
    this.securityGroups = securityGroups;
  }

}
