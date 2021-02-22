/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class RestoreDBInstanceFromDBSnapshotResponseType extends RdsMessage {

  private RestoreDBInstanceFromDBSnapshotResult result = new RestoreDBInstanceFromDBSnapshotResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public RestoreDBInstanceFromDBSnapshotResult getRestoreDBInstanceFromDBSnapshotResult() {
    return result;
  }

  public void setRestoreDBInstanceFromDBSnapshotResult(final RestoreDBInstanceFromDBSnapshotResult result) {
    this.result = result;
  }

}
