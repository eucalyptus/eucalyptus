/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class StopDBInstanceResponseType extends RdsMessage {

  private StopDBInstanceResult result = new StopDBInstanceResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public StopDBInstanceResult getStopDBInstanceResult() {
    return result;
  }

  public void setStopDBInstanceResult(final StopDBInstanceResult result) {
    this.result = result;
  }

}
