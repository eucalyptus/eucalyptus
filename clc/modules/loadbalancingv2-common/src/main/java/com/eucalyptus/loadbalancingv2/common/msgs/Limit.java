/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Limit extends EucalyptusData {

  private String max;

  private String name;

  public String getMax() {
    return max;
  }

  public void setMax(final String max) {
    this.max = max;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

}
