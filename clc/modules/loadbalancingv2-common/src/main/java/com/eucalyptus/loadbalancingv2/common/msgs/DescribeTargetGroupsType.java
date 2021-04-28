/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;


public class DescribeTargetGroupsType extends Loadbalancingv2Message {

  private String loadBalancerArn;

  private String marker;

  private TargetGroupNames names;

  @FieldRange(min = 1, max = 400)
  private Integer pageSize;

  private TargetGroupArns targetGroupArns;

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

  public TargetGroupNames getNames() {
    return names;
  }

  public void setNames(final TargetGroupNames names) {
    this.names = names;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(final Integer pageSize) {
    this.pageSize = pageSize;
  }

  public TargetGroupArns getTargetGroupArns() {
    return targetGroupArns;
  }

  public void setTargetGroupArns(final TargetGroupArns targetGroupArns) {
    this.targetGroupArns = targetGroupArns;
  }

}
