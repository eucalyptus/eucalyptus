/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBClusterBacktracksResult extends EucalyptusData {

  private DBClusterBacktrackList dBClusterBacktracks;

  private String marker;

  public DBClusterBacktrackList getDBClusterBacktracks() {
    return dBClusterBacktracks;
  }

  public void setDBClusterBacktracks(final DBClusterBacktrackList dBClusterBacktracks) {
    this.dBClusterBacktracks = dBClusterBacktracks;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
