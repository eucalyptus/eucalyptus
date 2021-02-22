/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class StopDBClusterResponseType extends RdsMessage {

  private StopDBClusterResult result = new StopDBClusterResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public StopDBClusterResult getStopDBClusterResult() {
    return result;
  }

  public void setStopDBClusterResult(final StopDBClusterResult result) {
    this.result = result;
  }

}
