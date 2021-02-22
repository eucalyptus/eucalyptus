/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DeleteDBClusterResponseType extends RdsMessage {

  private DeleteDBClusterResult result = new DeleteDBClusterResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeleteDBClusterResult getDeleteDBClusterResult() {
    return result;
  }

  public void setDeleteDBClusterResult(final DeleteDBClusterResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
