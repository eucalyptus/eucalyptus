/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBProxyTargetsResult extends EucalyptusData {

  private String marker;

  private TargetList targets;

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public TargetList getTargets() {
    return targets;
  }

  public void setTargets(final TargetList targets) {
    this.targets = targets;
  }

}
