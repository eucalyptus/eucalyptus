/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DeleteDBInstanceResponseType extends RdsMessage {

  private DeleteDBInstanceResult result = new DeleteDBInstanceResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeleteDBInstanceResult getDeleteDBInstanceResult() {
    return result;
  }

  public void setDeleteDBInstanceResult(final DeleteDBInstanceResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
