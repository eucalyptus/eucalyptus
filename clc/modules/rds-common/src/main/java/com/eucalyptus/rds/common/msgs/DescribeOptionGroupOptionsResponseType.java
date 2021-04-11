/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeOptionGroupOptionsResponseType extends RdsMessage {

  private DescribeOptionGroupOptionsResult result = new DescribeOptionGroupOptionsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeOptionGroupOptionsResult getDescribeOptionGroupOptionsResult() {
    return result;
  }

  public void setDescribeOptionGroupOptionsResult(final DescribeOptionGroupOptionsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
