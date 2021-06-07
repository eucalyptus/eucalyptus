/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DownloadDBLogFilePortionResponseType extends RdsMessage {

  private DownloadDBLogFilePortionResult result = new DownloadDBLogFilePortionResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DownloadDBLogFilePortionResult getDownloadDBLogFilePortionResult() {
    return result;
  }

  public void setDownloadDBLogFilePortionResult(final DownloadDBLogFilePortionResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
