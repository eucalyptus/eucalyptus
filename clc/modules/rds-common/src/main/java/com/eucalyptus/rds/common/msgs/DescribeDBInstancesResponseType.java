/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBInstancesResponseType extends RdsMessage {

  private DescribeDBInstancesResult result = new DescribeDBInstancesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeDBInstancesResult getDescribeDBInstancesResult() {
    return result;
  }

  public void setDescribeDBInstancesResult(final DescribeDBInstancesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
