/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateDBClusterSnapshotResponseType extends RdsMessage {

  private CreateDBClusterSnapshotResult result = new CreateDBClusterSnapshotResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateDBClusterSnapshotResult getCreateDBClusterSnapshotResult() {
    return result;
  }

  public void setCreateDBClusterSnapshotResult(final CreateDBClusterSnapshotResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
