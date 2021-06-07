/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DeleteGlobalClusterResponseType extends RdsMessage {

  private DeleteGlobalClusterResult result = new DeleteGlobalClusterResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeleteGlobalClusterResult getDeleteGlobalClusterResult() {
    return result;
  }

  public void setDeleteGlobalClusterResult(final DeleteGlobalClusterResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
