/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;


public class DescribeListenersType extends Loadbalancingv2Message {

  private ListenerArns listenerArns;

  private String loadBalancerArn;

  private String marker;

  @FieldRange(min = 1, max = 400)
  private Integer pageSize;

  public ListenerArns getListenerArns() {
    return listenerArns;
  }

  public void setListenerArns(final ListenerArns listenerArns) {
    this.listenerArns = listenerArns;
  }

  public String getLoadBalancerArn() {
    return loadBalancerArn;
  }

  public void setLoadBalancerArn(final String loadBalancerArn) {
    this.loadBalancerArn = loadBalancerArn;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(final Integer pageSize) {
    this.pageSize = pageSize;
  }

}
