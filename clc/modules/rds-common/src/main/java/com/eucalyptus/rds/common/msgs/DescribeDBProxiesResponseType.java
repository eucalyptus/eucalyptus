/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBProxiesResponseType extends RdsMessage {

  private DescribeDBProxiesResult result = new DescribeDBProxiesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBProxiesResult getDescribeDBProxiesResult() {
    return result;
  }

  public void setDescribeDBProxiesResult(final DescribeDBProxiesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
