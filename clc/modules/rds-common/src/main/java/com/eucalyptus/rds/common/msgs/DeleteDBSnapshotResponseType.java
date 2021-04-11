/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DeleteDBSnapshotResponseType extends RdsMessage {

  private DeleteDBSnapshotResult result = new DeleteDBSnapshotResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeleteDBSnapshotResult getDeleteDBSnapshotResult() {
    return result;
  }

  public void setDeleteDBSnapshotResult(final DeleteDBSnapshotResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
