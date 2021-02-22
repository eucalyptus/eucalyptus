/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeReservedDBInstancesResult extends EucalyptusData {

  private String marker;

  private ReservedDBInstanceList reservedDBInstances;

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public ReservedDBInstanceList getReservedDBInstances() {
    return reservedDBInstances;
  }

  public void setReservedDBInstances(final ReservedDBInstanceList reservedDBInstances) {
    this.reservedDBInstances = reservedDBInstances;
  }

}
