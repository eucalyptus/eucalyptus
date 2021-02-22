/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBParametersResponseType extends RdsMessage {

  private DescribeDBParametersResult result = new DescribeDBParametersResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBParametersResult getDescribeDBParametersResult() {
    return result;
  }

  public void setDescribeDBParametersResult(final DescribeDBParametersResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
