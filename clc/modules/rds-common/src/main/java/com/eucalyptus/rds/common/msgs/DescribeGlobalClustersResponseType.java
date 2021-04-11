/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeGlobalClustersResponseType extends RdsMessage {

  private DescribeGlobalClustersResult result = new DescribeGlobalClustersResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeGlobalClustersResult getDescribeGlobalClustersResult() {
    return result;
  }

  public void setDescribeGlobalClustersResult(final DescribeGlobalClustersResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
