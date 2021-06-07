/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeEventSubscriptionsResponseType extends RdsMessage {

  private DescribeEventSubscriptionsResult result = new DescribeEventSubscriptionsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeEventSubscriptionsResult getDescribeEventSubscriptionsResult() {
    return result;
  }

  public void setDescribeEventSubscriptionsResult(final DescribeEventSubscriptionsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
