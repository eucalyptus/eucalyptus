/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeSSLPoliciesResult extends EucalyptusData {

  private String nextMarker;

  private SslPolicies sslPolicies;

  public String getNextMarker() {
    return nextMarker;
  }

  public void setNextMarker(final String nextMarker) {
    this.nextMarker = nextMarker;
  }

  public SslPolicies getSslPolicies() {
    return sslPolicies;
  }

  public void setSslPolicies(final SslPolicies sslPolicies) {
    this.sslPolicies = sslPolicies;
  }

}
