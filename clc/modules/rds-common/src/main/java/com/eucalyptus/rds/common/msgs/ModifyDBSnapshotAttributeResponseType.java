/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyDBSnapshotAttributeResponseType extends RdsMessage {

  private ModifyDBSnapshotAttributeResult result = new ModifyDBSnapshotAttributeResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyDBSnapshotAttributeResult getModifyDBSnapshotAttributeResult() {
    return result;
  }

  public void setModifyDBSnapshotAttributeResult(final ModifyDBSnapshotAttributeResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
