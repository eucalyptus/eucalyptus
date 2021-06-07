/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBClusterSnapshotsResult extends EucalyptusData {

  private DBClusterSnapshotList dBClusterSnapshots;

  private String marker;

  public DBClusterSnapshotList getDBClusterSnapshots() {
    return dBClusterSnapshots;
  }

  public void setDBClusterSnapshots(final DBClusterSnapshotList dBClusterSnapshots) {
    this.dBClusterSnapshots = dBClusterSnapshots;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
