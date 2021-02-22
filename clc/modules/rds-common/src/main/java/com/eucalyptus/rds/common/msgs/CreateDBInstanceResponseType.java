/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateDBInstanceResponseType extends RdsMessage {

  private CreateDBInstanceResult result = new CreateDBInstanceResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateDBInstanceResult getCreateDBInstanceResult() {
    return result;
  }

  public void setCreateDBInstanceResult(final CreateDBInstanceResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
