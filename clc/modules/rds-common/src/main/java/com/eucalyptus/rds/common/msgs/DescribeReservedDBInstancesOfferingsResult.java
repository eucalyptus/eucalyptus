/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeReservedDBInstancesOfferingsResult extends EucalyptusData {

  private String marker;

  private ReservedDBInstancesOfferingList reservedDBInstancesOfferings;

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public ReservedDBInstancesOfferingList getReservedDBInstancesOfferings() {
    return reservedDBInstancesOfferings;
  }

  public void setReservedDBInstancesOfferings(final ReservedDBInstancesOfferingList reservedDBInstancesOfferings) {
    this.reservedDBInstancesOfferings = reservedDBInstancesOfferings;
  }

}
