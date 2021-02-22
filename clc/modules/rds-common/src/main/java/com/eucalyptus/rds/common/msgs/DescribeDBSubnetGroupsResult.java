/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBSubnetGroupsResult extends EucalyptusData {

  private DBSubnetGroups dBSubnetGroups;

  private String marker;

  public DBSubnetGroups getDBSubnetGroups() {
    return dBSubnetGroups;
  }

  public void setDBSubnetGroups(final DBSubnetGroups dBSubnetGroups) {
    this.dBSubnetGroups = dBSubnetGroups;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
