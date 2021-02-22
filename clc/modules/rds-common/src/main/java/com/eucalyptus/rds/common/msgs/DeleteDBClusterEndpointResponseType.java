/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DeleteDBClusterEndpointResponseType extends RdsMessage {

  private DeleteDBClusterEndpointResult result = new DeleteDBClusterEndpointResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeleteDBClusterEndpointResult getDeleteDBClusterEndpointResult() {
    return result;
  }

  public void setDeleteDBClusterEndpointResult(final DeleteDBClusterEndpointResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
