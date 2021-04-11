/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBClusterParameterGroupsResult extends EucalyptusData {

  private DBClusterParameterGroupList dBClusterParameterGroups;

  private String marker;

  public DBClusterParameterGroupList getDBClusterParameterGroups() {
    return dBClusterParameterGroups;
  }

  public void setDBClusterParameterGroups(final DBClusterParameterGroupList dBClusterParameterGroups) {
    this.dBClusterParameterGroups = dBClusterParameterGroups;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
