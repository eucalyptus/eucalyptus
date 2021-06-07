/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribePendingMaintenanceActionsResponseType extends RdsMessage {

  private DescribePendingMaintenanceActionsResult result = new DescribePendingMaintenanceActionsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribePendingMaintenanceActionsResult getDescribePendingMaintenanceActionsResult() {
    return result;
  }

  public void setDescribePendingMaintenanceActionsResult(final DescribePendingMaintenanceActionsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
