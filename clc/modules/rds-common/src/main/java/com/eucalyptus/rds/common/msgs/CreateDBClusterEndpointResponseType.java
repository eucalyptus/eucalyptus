/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateDBClusterEndpointResponseType extends RdsMessage {

  private CreateDBClusterEndpointResult result = new CreateDBClusterEndpointResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateDBClusterEndpointResult getCreateDBClusterEndpointResult() {
    return result;
  }

  public void setCreateDBClusterEndpointResult(final CreateDBClusterEndpointResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
