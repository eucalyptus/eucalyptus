/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CreateCustomAvailabilityZoneResult extends EucalyptusData {

  private CustomAvailabilityZone customAvailabilityZone;

  public CustomAvailabilityZone getCustomAvailabilityZone() {
    return customAvailabilityZone;
  }

  public void setCustomAvailabilityZone(final CustomAvailabilityZone customAvailabilityZone) {
    this.customAvailabilityZone = customAvailabilityZone;
  }

}
