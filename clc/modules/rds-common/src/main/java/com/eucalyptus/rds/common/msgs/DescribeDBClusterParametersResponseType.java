/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBClusterParametersResponseType extends RdsMessage {

  private DescribeDBClusterParametersResult result = new DescribeDBClusterParametersResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBClusterParametersResult getDescribeDBClusterParametersResult() {
    return result;
  }

  public void setDescribeDBClusterParametersResult(final DescribeDBClusterParametersResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
