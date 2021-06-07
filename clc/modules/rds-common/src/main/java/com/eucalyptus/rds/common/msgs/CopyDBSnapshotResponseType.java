/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CopyDBSnapshotResponseType extends RdsMessage {

  private CopyDBSnapshotResult result = new CopyDBSnapshotResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CopyDBSnapshotResult getCopyDBSnapshotResult() {
    return result;
  }

  public void setCopyDBSnapshotResult(final CopyDBSnapshotResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
