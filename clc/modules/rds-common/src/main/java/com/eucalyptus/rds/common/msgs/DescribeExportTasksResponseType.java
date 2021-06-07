/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeExportTasksResponseType extends RdsMessage {

  private DescribeExportTasksResult result = new DescribeExportTasksResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeExportTasksResult getDescribeExportTasksResult() {
    return result;
  }

  public void setDescribeExportTasksResult(final DescribeExportTasksResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
