/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateGlobalClusterResponseType extends RdsMessage {

  private CreateGlobalClusterResult result = new CreateGlobalClusterResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateGlobalClusterResult getCreateGlobalClusterResult() {
    return result;
  }

  public void setCreateGlobalClusterResult(final CreateGlobalClusterResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
