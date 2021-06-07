/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class PurchaseReservedDBInstancesOfferingResult extends EucalyptusData {

  private ReservedDBInstance reservedDBInstance;

  public ReservedDBInstance getReservedDBInstance() {
    return reservedDBInstance;
  }

  public void setReservedDBInstance(final ReservedDBInstance reservedDBInstance) {
    this.reservedDBInstance = reservedDBInstance;
  }

}
