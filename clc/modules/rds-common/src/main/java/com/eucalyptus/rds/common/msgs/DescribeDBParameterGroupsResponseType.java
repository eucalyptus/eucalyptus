/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBParameterGroupsResponseType extends RdsMessage {

  private DescribeDBParameterGroupsResult result = new DescribeDBParameterGroupsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBParameterGroupsResult getDescribeDBParameterGroupsResult() {
    return result;
  }

  public void setDescribeDBParameterGroupsResult(final DescribeDBParameterGroupsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
