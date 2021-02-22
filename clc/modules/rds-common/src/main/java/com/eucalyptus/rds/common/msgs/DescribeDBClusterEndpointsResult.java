/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBClusterEndpointsResult extends EucalyptusData {

  private DBClusterEndpointList dBClusterEndpoints;

  private String marker;

  public DBClusterEndpointList getDBClusterEndpoints() {
    return dBClusterEndpoints;
  }

  public void setDBClusterEndpoints(final DBClusterEndpointList dBClusterEndpoints) {
    this.dBClusterEndpoints = dBClusterEndpoints;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
