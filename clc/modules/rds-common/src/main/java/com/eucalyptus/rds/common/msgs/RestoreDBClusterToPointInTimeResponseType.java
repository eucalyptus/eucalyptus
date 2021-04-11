/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class RestoreDBClusterToPointInTimeResponseType extends RdsMessage {

  private RestoreDBClusterToPointInTimeResult result = new RestoreDBClusterToPointInTimeResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public RestoreDBClusterToPointInTimeResult getRestoreDBClusterToPointInTimeResult() {
    return result;
  }

  public void setRestoreDBClusterToPointInTimeResult(final RestoreDBClusterToPointInTimeResult result) {
    this.result = result;
  }

}
