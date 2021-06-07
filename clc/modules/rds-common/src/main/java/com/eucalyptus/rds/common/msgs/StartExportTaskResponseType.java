/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class StartExportTaskResponseType extends RdsMessage {

  private StartExportTaskResult result = new StartExportTaskResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public StartExportTaskResult getStartExportTaskResult() {
    return result;
  }

  public void setStartExportTaskResult(final StartExportTaskResult result) {
    this.result = result;
  }

}
