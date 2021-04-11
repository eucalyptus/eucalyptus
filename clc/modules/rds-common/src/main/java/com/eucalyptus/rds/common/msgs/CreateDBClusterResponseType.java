/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateDBClusterResponseType extends RdsMessage {

  private CreateDBClusterResult result = new CreateDBClusterResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateDBClusterResult getCreateDBClusterResult() {
    return result;
  }

  public void setCreateDBClusterResult(final CreateDBClusterResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
