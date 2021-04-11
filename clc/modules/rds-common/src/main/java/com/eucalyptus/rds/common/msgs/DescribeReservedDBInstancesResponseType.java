/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeReservedDBInstancesResponseType extends RdsMessage {

  private DescribeReservedDBInstancesResult result = new DescribeReservedDBInstancesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeReservedDBInstancesResult getDescribeReservedDBInstancesResult() {
    return result;
  }

  public void setDescribeReservedDBInstancesResult(final DescribeReservedDBInstancesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
