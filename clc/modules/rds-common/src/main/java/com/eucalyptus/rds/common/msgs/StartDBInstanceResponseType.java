/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class StartDBInstanceResponseType extends RdsMessage {

  private StartDBInstanceResult result = new StartDBInstanceResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public StartDBInstanceResult getStartDBInstanceResult() {
    return result;
  }

  public void setStartDBInstanceResult(final StartDBInstanceResult result) {
    this.result = result;
  }

}
