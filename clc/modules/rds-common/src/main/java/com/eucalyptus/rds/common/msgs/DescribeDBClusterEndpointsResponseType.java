/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBClusterEndpointsResponseType extends RdsMessage {

  private DescribeDBClusterEndpointsResult result = new DescribeDBClusterEndpointsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBClusterEndpointsResult getDescribeDBClusterEndpointsResult() {
    return result;
  }

  public void setDescribeDBClusterEndpointsResult(final DescribeDBClusterEndpointsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
