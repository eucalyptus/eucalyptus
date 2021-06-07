/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CancelExportTaskResponseType extends RdsMessage {

  private CancelExportTaskResult result = new CancelExportTaskResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CancelExportTaskResult getCancelExportTaskResult() {
    return result;
  }

  public void setCancelExportTaskResult(final CancelExportTaskResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
