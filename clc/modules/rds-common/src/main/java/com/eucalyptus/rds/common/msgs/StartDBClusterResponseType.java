/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class StartDBClusterResponseType extends RdsMessage {

  private StartDBClusterResult result = new StartDBClusterResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public StartDBClusterResult getStartDBClusterResult() {
    return result;
  }

  public void setStartDBClusterResult(final StartDBClusterResult result) {
    this.result = result;
  }

}
