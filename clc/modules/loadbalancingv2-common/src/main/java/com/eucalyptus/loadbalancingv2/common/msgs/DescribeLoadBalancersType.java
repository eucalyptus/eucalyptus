/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;


public class DescribeLoadBalancersType extends Loadbalancingv2Message {

  private LoadBalancerArns loadBalancerArns;

  private String marker;

  private LoadBalancerNames names;

  @FieldRange(min = 1, max = 400)
  private Integer pageSize;

  public LoadBalancerArns getLoadBalancerArns() {
    return loadBalancerArns;
  }

  public void setLoadBalancerArns(final LoadBalancerArns loadBalancerArns) {
    this.loadBalancerArns = loadBalancerArns;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public LoadBalancerNames getNames() {
    return names;
  }

  public void setNames(final LoadBalancerNames names) {
    this.names = names;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(final Integer pageSize) {
    this.pageSize = pageSize;
  }

}
