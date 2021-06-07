/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBSecurityGroupsResult extends EucalyptusData {

  private DBSecurityGroups dBSecurityGroups;

  private String marker;

  public DBSecurityGroups getDBSecurityGroups() {
    return dBSecurityGroups;
  }

  public void setDBSecurityGroups(final DBSecurityGroups dBSecurityGroups) {
    this.dBSecurityGroups = dBSecurityGroups;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
