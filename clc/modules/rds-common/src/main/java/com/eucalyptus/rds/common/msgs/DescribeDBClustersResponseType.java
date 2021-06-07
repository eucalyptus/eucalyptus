/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBClustersResponseType extends RdsMessage {

  private DescribeDBClustersResult result = new DescribeDBClustersResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBClustersResult getDescribeDBClustersResult() {
    return result;
  }

  public void setDescribeDBClustersResult(final DescribeDBClustersResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
