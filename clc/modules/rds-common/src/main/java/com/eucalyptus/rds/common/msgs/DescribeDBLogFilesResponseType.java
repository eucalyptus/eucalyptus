/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBLogFilesResponseType extends RdsMessage {

  private DescribeDBLogFilesResult result = new DescribeDBLogFilesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBLogFilesResult getDescribeDBLogFilesResult() {
    return result;
  }

  public void setDescribeDBLogFilesResult(final DescribeDBLogFilesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
