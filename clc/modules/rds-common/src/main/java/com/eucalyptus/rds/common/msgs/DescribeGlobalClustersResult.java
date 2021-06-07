/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeGlobalClustersResult extends EucalyptusData {

  private GlobalClusterList globalClusters;

  private String marker;

  public GlobalClusterList getGlobalClusters() {
    return globalClusters;
  }

  public void setGlobalClusters(final GlobalClusterList globalClusters) {
    this.globalClusters = globalClusters;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
