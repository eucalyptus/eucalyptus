/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;


public class DeleteLoadBalancerType extends Loadbalancingv2Message {

  @Nonnull
  private String loadBalancerArn;

  public String getLoadBalancerArn() {
    return loadBalancerArn;
  }

  public void setLoadBalancerArn(final String loadBalancerArn) {
    this.loadBalancerArn = loadBalancerArn;
  }

}
