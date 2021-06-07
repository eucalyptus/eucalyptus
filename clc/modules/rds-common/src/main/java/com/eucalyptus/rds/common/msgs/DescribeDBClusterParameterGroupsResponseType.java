/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBClusterParameterGroupsResponseType extends RdsMessage {

  private DescribeDBClusterParameterGroupsResult result = new DescribeDBClusterParameterGroupsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBClusterParameterGroupsResult getDescribeDBClusterParameterGroupsResult() {
    return result;
  }

  public void setDescribeDBClusterParameterGroupsResult(final DescribeDBClusterParameterGroupsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
