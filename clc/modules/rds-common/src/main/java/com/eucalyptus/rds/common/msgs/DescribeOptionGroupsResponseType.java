/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeOptionGroupsResponseType extends RdsMessage {

  private DescribeOptionGroupsResult result = new DescribeOptionGroupsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeOptionGroupsResult getDescribeOptionGroupsResult() {
    return result;
  }

  public void setDescribeOptionGroupsResult(final DescribeOptionGroupsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
