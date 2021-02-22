/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBEngineVersionsResult extends EucalyptusData {

  private DBEngineVersionList dBEngineVersions;

  private String marker;

  public DBEngineVersionList getDBEngineVersions() {
    return dBEngineVersions;
  }

  public void setDBEngineVersions(final DBEngineVersionList dBEngineVersions) {
    this.dBEngineVersions = dBEngineVersions;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
