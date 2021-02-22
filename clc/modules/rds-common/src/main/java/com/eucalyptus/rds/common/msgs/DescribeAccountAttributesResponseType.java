/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeAccountAttributesResponseType extends RdsMessage {

  private DescribeAccountAttributesResult result = new DescribeAccountAttributesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeAccountAttributesResult getDescribeAccountAttributesResult() {
    return result;
  }

  public void setDescribeAccountAttributesResult(final DescribeAccountAttributesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
