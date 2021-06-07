/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class StopActivityStreamResponseType extends RdsMessage {

  private StopActivityStreamResult result = new StopActivityStreamResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public StopActivityStreamResult getStopActivityStreamResult() {
    return result;
  }

  public void setStopActivityStreamResult(final StopActivityStreamResult result) {
    this.result = result;
  }

}
