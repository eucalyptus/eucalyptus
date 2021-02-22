/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateDBParameterGroupResponseType extends RdsMessage {

  private CreateDBParameterGroupResult result = new CreateDBParameterGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateDBParameterGroupResult getCreateDBParameterGroupResult() {
    return result;
  }

  public void setCreateDBParameterGroupResult(final CreateDBParameterGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
