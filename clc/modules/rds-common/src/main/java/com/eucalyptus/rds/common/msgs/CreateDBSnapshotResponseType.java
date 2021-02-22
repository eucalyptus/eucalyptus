/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateDBSnapshotResponseType extends RdsMessage {

  private CreateDBSnapshotResult result = new CreateDBSnapshotResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateDBSnapshotResult getCreateDBSnapshotResult() {
    return result;
  }

  public void setCreateDBSnapshotResult(final CreateDBSnapshotResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
