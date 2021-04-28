/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeListenersResult extends EucalyptusData {

  private Listeners listeners;

  private String nextMarker;

  public Listeners getListeners() {
    return listeners;
  }

  public void setListeners(final Listeners listeners) {
    this.listeners = listeners;
  }

  public String getNextMarker() {
    return nextMarker;
  }

  public void setNextMarker(final String nextMarker) {
    this.nextMarker = nextMarker;
  }

}
