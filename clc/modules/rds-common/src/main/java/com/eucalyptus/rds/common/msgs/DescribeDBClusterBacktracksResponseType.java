/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBClusterBacktracksResponseType extends RdsMessage {

  private DescribeDBClusterBacktracksResult result = new DescribeDBClusterBacktracksResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBClusterBacktracksResult getDescribeDBClusterBacktracksResult() {
    return result;
  }

  public void setDescribeDBClusterBacktracksResult(final DescribeDBClusterBacktracksResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
