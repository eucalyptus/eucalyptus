/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class RestoreDBInstanceToPointInTimeResponseType extends RdsMessage {

  private RestoreDBInstanceToPointInTimeResult result = new RestoreDBInstanceToPointInTimeResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public RestoreDBInstanceToPointInTimeResult getRestoreDBInstanceToPointInTimeResult() {
    return result;
  }

  public void setRestoreDBInstanceToPointInTimeResult(final RestoreDBInstanceToPointInTimeResult result) {
    this.result = result;
  }

}
