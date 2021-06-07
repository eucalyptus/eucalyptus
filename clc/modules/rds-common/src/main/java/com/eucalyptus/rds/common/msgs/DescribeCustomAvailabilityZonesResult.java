/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeCustomAvailabilityZonesResult extends EucalyptusData {

  private CustomAvailabilityZoneList customAvailabilityZones;

  private String marker;

  public CustomAvailabilityZoneList getCustomAvailabilityZones() {
    return customAvailabilityZones;
  }

  public void setCustomAvailabilityZones(final CustomAvailabilityZoneList customAvailabilityZones) {
    this.customAvailabilityZones = customAvailabilityZones;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
