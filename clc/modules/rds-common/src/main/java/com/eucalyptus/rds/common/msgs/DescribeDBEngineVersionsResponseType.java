/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBEngineVersionsResponseType extends RdsMessage {

  private DescribeDBEngineVersionsResult result = new DescribeDBEngineVersionsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBEngineVersionsResult getDescribeDBEngineVersionsResult() {
    return result;
  }

  public void setDescribeDBEngineVersionsResult(final DescribeDBEngineVersionsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
