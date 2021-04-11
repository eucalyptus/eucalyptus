/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyGlobalClusterResponseType extends RdsMessage {

  private ModifyGlobalClusterResult result = new ModifyGlobalClusterResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyGlobalClusterResult getModifyGlobalClusterResult() {
    return result;
  }

  public void setModifyGlobalClusterResult(final ModifyGlobalClusterResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
