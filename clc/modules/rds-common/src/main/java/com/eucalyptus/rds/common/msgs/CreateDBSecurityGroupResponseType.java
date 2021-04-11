/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateDBSecurityGroupResponseType extends RdsMessage {

  private CreateDBSecurityGroupResult result = new CreateDBSecurityGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateDBSecurityGroupResult getCreateDBSecurityGroupResult() {
    return result;
  }

  public void setCreateDBSecurityGroupResult(final CreateDBSecurityGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
