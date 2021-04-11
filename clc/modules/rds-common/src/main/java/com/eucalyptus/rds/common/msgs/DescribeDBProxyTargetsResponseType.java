/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBProxyTargetsResponseType extends RdsMessage {

  private DescribeDBProxyTargetsResult result = new DescribeDBProxyTargetsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBProxyTargetsResult getDescribeDBProxyTargetsResult() {
    return result;
  }

  public void setDescribeDBProxyTargetsResult(final DescribeDBProxyTargetsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
