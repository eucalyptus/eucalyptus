/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegex;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRegexValue;


public class SetIpAddressTypeType extends Loadbalancingv2Message {

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_IPADDRESSTYPE)
  private String ipAddressType;

  @Nonnull
  private String loadBalancerArn;

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

}
