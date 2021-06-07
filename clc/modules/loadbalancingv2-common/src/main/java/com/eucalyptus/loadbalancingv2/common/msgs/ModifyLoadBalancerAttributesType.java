/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;


public class ModifyLoadBalancerAttributesType extends Loadbalancingv2Message {

  @Nonnull
  private LoadBalancerAttributes attributes;

  @Nonnull
  private String loadBalancerArn;

  public LoadBalancerAttributes getAttributes() {
    return attributes;
  }

  public void setAttributes(final LoadBalancerAttributes attributes) {
    this.attributes = attributes;
  }

  public String getLoadBalancerArn() {
    return loadBalancerArn;
  }

  public void setLoadBalancerArn(final String loadBalancerArn) {
    this.loadBalancerArn = loadBalancerArn;
  }

}
