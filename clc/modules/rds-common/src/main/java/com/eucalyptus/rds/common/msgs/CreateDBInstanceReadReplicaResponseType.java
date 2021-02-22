/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateDBInstanceReadReplicaResponseType extends RdsMessage {

  private CreateDBInstanceReadReplicaResult result = new CreateDBInstanceReadReplicaResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateDBInstanceReadReplicaResult getCreateDBInstanceReadReplicaResult() {
    return result;
  }

  public void setCreateDBInstanceReadReplicaResult(final CreateDBInstanceReadReplicaResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
