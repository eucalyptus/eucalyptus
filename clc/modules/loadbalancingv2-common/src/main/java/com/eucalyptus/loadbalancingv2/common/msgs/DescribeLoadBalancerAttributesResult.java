/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeLoadBalancerAttributesResult extends EucalyptusData {

  @FieldRange(max = 20)
  private LoadBalancerAttributes attributes;

  public LoadBalancerAttributes getAttributes() {
    return attributes;
  }

  public void setAttributes(final LoadBalancerAttributes attributes) {
    this.attributes = attributes;
  }

}
