/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class PurchaseReservedDBInstancesOfferingResponseType extends RdsMessage {

  private PurchaseReservedDBInstancesOfferingResult result = new PurchaseReservedDBInstancesOfferingResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public PurchaseReservedDBInstancesOfferingResult getPurchaseReservedDBInstancesOfferingResult() {
    return result;
  }

  public void setPurchaseReservedDBInstancesOfferingResult(final PurchaseReservedDBInstancesOfferingResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
