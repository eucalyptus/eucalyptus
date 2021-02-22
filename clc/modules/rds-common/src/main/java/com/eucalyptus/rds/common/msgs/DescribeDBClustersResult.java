/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBClustersResult extends EucalyptusData {

  private DBClusterList dBClusters;

  private String marker;

  public DBClusterList getDBClusters() {
    return dBClusters;
  }

  public void setDBClusters(final DBClusterList dBClusters) {
    this.dBClusters = dBClusters;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
