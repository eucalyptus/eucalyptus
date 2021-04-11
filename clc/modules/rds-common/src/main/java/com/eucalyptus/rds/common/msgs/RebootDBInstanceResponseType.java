/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class RebootDBInstanceResponseType extends RdsMessage {

  private RebootDBInstanceResult result = new RebootDBInstanceResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public RebootDBInstanceResult getRebootDBInstanceResult() {
    return result;
  }

  public void setRebootDBInstanceResult(final RebootDBInstanceResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
