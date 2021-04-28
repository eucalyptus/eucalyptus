/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;


public class DescribeAccountLimitsType extends Loadbalancingv2Message {

  private String marker;

  @FieldRange(min = 1, max = 400)
  private Integer pageSize;

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
