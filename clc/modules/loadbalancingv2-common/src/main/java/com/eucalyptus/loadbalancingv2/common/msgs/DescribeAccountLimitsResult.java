/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeAccountLimitsResult extends EucalyptusData {

  private Limits limits;

  private String nextMarker;

  public Limits getLimits() {
    return limits;
  }

  public void setLimits(final Limits limits) {
    this.limits = limits;
  }

  public String getNextMarker() {
    return nextMarker;
  }

  public void setNextMarker(final String nextMarker) {
    this.nextMarker = nextMarker;
  }

}
