/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBSnapshotsResult extends EucalyptusData {

  private DBSnapshotList dBSnapshots;

  private String marker;

  public DBSnapshotList getDBSnapshots() {
    return dBSnapshots;
  }

  public void setDBSnapshots(final DBSnapshotList dBSnapshots) {
    this.dBSnapshots = dBSnapshots;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
