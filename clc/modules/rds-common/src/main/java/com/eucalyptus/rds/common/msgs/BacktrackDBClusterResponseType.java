/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class BacktrackDBClusterResponseType extends RdsMessage {

  private BacktrackDBClusterResult result = new BacktrackDBClusterResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public BacktrackDBClusterResult getBacktrackDBClusterResult() {
    return result;
  }

  public void setBacktrackDBClusterResult(final BacktrackDBClusterResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
