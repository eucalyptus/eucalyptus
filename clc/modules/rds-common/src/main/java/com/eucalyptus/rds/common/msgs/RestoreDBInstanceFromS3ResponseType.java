/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class RestoreDBInstanceFromS3ResponseType extends RdsMessage {

  private RestoreDBInstanceFromS3Result result = new RestoreDBInstanceFromS3Result();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public RestoreDBInstanceFromS3Result getRestoreDBInstanceFromS3Result() {
    return result;
  }

  public void setRestoreDBInstanceFromS3Result(final RestoreDBInstanceFromS3Result result) {
    this.result = result;
  }

}
