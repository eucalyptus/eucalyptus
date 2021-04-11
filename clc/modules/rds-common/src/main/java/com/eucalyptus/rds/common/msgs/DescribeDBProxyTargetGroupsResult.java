/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBProxyTargetGroupsResult extends EucalyptusData {

  private String marker;

  private TargetGroupList targetGroups;

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public TargetGroupList getTargetGroups() {
    return targetGroups;
  }

  public void setTargetGroups(final TargetGroupList targetGroups) {
    this.targetGroups = targetGroups;
  }

}
