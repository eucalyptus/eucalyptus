/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBParameterGroupsResult extends EucalyptusData {

  private DBParameterGroupList dBParameterGroups;

  private String marker;

  public DBParameterGroupList getDBParameterGroups() {
    return dBParameterGroups;
  }

  public void setDBParameterGroups(final DBParameterGroupList dBParameterGroups) {
    this.dBParameterGroups = dBParameterGroups;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
