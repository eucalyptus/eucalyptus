/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CopyDBClusterSnapshotResponseType extends RdsMessage {

  private CopyDBClusterSnapshotResult result = new CopyDBClusterSnapshotResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CopyDBClusterSnapshotResult getCopyDBClusterSnapshotResult() {
    return result;
  }

  public void setCopyDBClusterSnapshotResult(final CopyDBClusterSnapshotResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
