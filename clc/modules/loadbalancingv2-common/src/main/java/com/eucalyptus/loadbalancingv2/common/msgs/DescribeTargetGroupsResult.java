/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeTargetGroupsResult extends EucalyptusData {

  private String nextMarker;

  private TargetGroups targetGroups;

  public String getNextMarker() {
    return nextMarker;
  }

  public void setNextMarker(final String nextMarker) {
    this.nextMarker = nextMarker;
  }

  public TargetGroups getTargetGroups() {
    return targetGroups;
  }

  public void setTargetGroups(final TargetGroups targetGroups) {
    this.targetGroups = targetGroups;
  }

}
