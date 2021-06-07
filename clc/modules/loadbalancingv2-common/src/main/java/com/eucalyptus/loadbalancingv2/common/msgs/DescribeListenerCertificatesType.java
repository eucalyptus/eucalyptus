/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation.FieldRange;


public class DescribeListenerCertificatesType extends Loadbalancingv2Message {

  @Nonnull
  private String listenerArn;

  private String marker;

  @FieldRange(min = 1, max = 400)
  private Integer pageSize;

  public String getListenerArn() {
    return listenerArn;
  }

  public void setListenerArn(final String listenerArn) {
    this.listenerArn = listenerArn;
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
