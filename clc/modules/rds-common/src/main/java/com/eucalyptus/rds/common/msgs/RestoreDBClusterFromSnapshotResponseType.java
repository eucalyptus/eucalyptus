/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class RestoreDBClusterFromSnapshotResponseType extends RdsMessage {

  private RestoreDBClusterFromSnapshotResult result = new RestoreDBClusterFromSnapshotResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public RestoreDBClusterFromSnapshotResult getRestoreDBClusterFromSnapshotResult() {
    return result;
  }

  public void setRestoreDBClusterFromSnapshotResult(final RestoreDBClusterFromSnapshotResult result) {
    this.result = result;
  }

}
