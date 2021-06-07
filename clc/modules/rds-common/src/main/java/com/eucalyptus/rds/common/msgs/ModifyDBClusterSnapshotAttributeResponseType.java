/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyDBClusterSnapshotAttributeResponseType extends RdsMessage {

  private ModifyDBClusterSnapshotAttributeResult result = new ModifyDBClusterSnapshotAttributeResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyDBClusterSnapshotAttributeResult getModifyDBClusterSnapshotAttributeResult() {
    return result;
  }

  public void setModifyDBClusterSnapshotAttributeResult(final ModifyDBClusterSnapshotAttributeResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
