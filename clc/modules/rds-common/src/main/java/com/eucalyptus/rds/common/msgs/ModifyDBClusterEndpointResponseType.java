/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyDBClusterEndpointResponseType extends RdsMessage {

  private ModifyDBClusterEndpointResult result = new ModifyDBClusterEndpointResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyDBClusterEndpointResult getModifyDBClusterEndpointResult() {
    return result;
  }

  public void setModifyDBClusterEndpointResult(final ModifyDBClusterEndpointResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
