/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeEventCategoriesResponseType extends RdsMessage {

  private DescribeEventCategoriesResult result = new DescribeEventCategoriesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeEventCategoriesResult getDescribeEventCategoriesResult() {
    return result;
  }

  public void setDescribeEventCategoriesResult(final DescribeEventCategoriesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
