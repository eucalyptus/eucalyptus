/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeSourceRegionsResult extends EucalyptusData {

  private String marker;

  private SourceRegionList sourceRegions;

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public SourceRegionList getSourceRegions() {
    return sourceRegions;
  }

  public void setSourceRegions(final SourceRegionList sourceRegions) {
    this.sourceRegions = sourceRegions;
  }

}
