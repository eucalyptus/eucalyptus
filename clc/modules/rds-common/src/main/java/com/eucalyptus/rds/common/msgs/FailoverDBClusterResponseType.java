/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class FailoverDBClusterResponseType extends RdsMessage {

  private FailoverDBClusterResult result = new FailoverDBClusterResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public FailoverDBClusterResult getFailoverDBClusterResult() {
    return result;
  }

  public void setFailoverDBClusterResult(final FailoverDBClusterResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
