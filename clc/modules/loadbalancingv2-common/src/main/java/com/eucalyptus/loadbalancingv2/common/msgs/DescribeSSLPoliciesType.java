/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;


public class DescribeSSLPoliciesType extends Loadbalancingv2Message {

  private String marker;

  private SslPolicyNames names;

  @FieldRange(min = 1, max = 400)
  private Integer pageSize;

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public SslPolicyNames getNames() {
    return names;
  }

  public void setNames(final SslPolicyNames names) {
    this.names = names;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(final Integer pageSize) {
    this.pageSize = pageSize;
  }

}
