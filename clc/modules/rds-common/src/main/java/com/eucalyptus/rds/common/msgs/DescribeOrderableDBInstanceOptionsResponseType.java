/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeOrderableDBInstanceOptionsResponseType extends RdsMessage {

  private DescribeOrderableDBInstanceOptionsResult result = new DescribeOrderableDBInstanceOptionsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeOrderableDBInstanceOptionsResult getDescribeOrderableDBInstanceOptionsResult() {
    return result;
  }

  public void setDescribeOrderableDBInstanceOptionsResult(final DescribeOrderableDBInstanceOptionsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
