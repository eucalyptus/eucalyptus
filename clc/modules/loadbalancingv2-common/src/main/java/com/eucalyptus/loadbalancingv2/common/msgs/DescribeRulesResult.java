/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeRulesResult extends EucalyptusData {

  private String nextMarker;

  private Rules rules;

  public String getNextMarker() {
    return nextMarker;
  }

  public void setNextMarker(final String nextMarker) {
    this.nextMarker = nextMarker;
  }

  public Rules getRules() {
    return rules;
  }

  public void setRules(final Rules rules) {
    this.rules = rules;
  }

}
