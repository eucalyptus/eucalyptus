/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateDBClusterParameterGroupResponseType extends RdsMessage {

  private CreateDBClusterParameterGroupResult result = new CreateDBClusterParameterGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateDBClusterParameterGroupResult getCreateDBClusterParameterGroupResult() {
    return result;
  }

  public void setCreateDBClusterParameterGroupResult(final CreateDBClusterParameterGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
