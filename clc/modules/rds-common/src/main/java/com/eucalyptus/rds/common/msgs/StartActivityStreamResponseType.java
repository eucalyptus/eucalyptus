/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class StartActivityStreamResponseType extends RdsMessage {

  private StartActivityStreamResult result = new StartActivityStreamResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public StartActivityStreamResult getStartActivityStreamResult() {
    return result;
  }

  public void setStartActivityStreamResult(final StartActivityStreamResult result) {
    this.result = result;
  }

}
