/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DeleteDBClusterSnapshotResponseType extends RdsMessage {

  private DeleteDBClusterSnapshotResult result = new DeleteDBClusterSnapshotResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeleteDBClusterSnapshotResult getDeleteDBClusterSnapshotResult() {
    return result;
  }

  public void setDeleteDBClusterSnapshotResult(final DeleteDBClusterSnapshotResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
