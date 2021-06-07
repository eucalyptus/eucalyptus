/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyDBClusterResponseType extends RdsMessage {

  private ModifyDBClusterResult result = new ModifyDBClusterResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyDBClusterResult getModifyDBClusterResult() {
    return result;
  }

  public void setModifyDBClusterResult(final ModifyDBClusterResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
