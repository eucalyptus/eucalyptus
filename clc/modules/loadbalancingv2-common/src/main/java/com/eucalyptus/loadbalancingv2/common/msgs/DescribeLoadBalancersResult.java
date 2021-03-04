/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeLoadBalancersResult extends EucalyptusData {

  private LoadBalancers loadBalancers;

  private String nextMarker;

  public LoadBalancers getLoadBalancers() {
    return loadBalancers;
  }

  public void setLoadBalancers(final LoadBalancers loadBalancers) {
    this.loadBalancers = loadBalancers;
  }

  public String getNextMarker() {
    return nextMarker;
  }

  public void setNextMarker(final String nextMarker) {
    this.nextMarker = nextMarker;
  }

}
