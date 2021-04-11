/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeEventsResponseType extends RdsMessage {

  private DescribeEventsResult result = new DescribeEventsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeEventsResult getDescribeEventsResult() {
    return result;
  }

  public void setDescribeEventsResult(final DescribeEventsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
