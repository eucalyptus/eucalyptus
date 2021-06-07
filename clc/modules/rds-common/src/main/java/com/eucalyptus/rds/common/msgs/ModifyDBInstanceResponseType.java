/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyDBInstanceResponseType extends RdsMessage {

  private ModifyDBInstanceResult result = new ModifyDBInstanceResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyDBInstanceResult getModifyDBInstanceResult() {
    return result;
  }

  public void setModifyDBInstanceResult(final ModifyDBInstanceResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
