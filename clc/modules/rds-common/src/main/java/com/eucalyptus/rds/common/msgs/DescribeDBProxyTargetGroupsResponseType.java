/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBProxyTargetGroupsResponseType extends RdsMessage {

  private DescribeDBProxyTargetGroupsResult result = new DescribeDBProxyTargetGroupsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBProxyTargetGroupsResult getDescribeDBProxyTargetGroupsResult() {
    return result;
  }

  public void setDescribeDBProxyTargetGroupsResult(final DescribeDBProxyTargetGroupsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
