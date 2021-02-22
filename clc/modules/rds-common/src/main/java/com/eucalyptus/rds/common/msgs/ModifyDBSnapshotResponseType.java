/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyDBSnapshotResponseType extends RdsMessage {

  private ModifyDBSnapshotResult result = new ModifyDBSnapshotResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyDBSnapshotResult getModifyDBSnapshotResult() {
    return result;
  }

  public void setModifyDBSnapshotResult(final ModifyDBSnapshotResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
