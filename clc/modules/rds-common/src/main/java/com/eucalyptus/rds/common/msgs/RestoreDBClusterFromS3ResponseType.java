/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class RestoreDBClusterFromS3ResponseType extends RdsMessage {

  private RestoreDBClusterFromS3Result result = new RestoreDBClusterFromS3Result();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public RestoreDBClusterFromS3Result getRestoreDBClusterFromS3Result() {
    return result;
  }

  public void setRestoreDBClusterFromS3Result(final RestoreDBClusterFromS3Result result) {
    this.result = result;
  }

}
