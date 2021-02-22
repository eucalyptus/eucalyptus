/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateDBProxyResponseType extends RdsMessage {

  private CreateDBProxyResult result = new CreateDBProxyResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateDBProxyResult getCreateDBProxyResult() {
    return result;
  }

  public void setCreateDBProxyResult(final CreateDBProxyResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
