/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DescribeTagsResponseType extends Loadbalancingv2Message {

  private DescribeTagsResult result = new DescribeTagsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeTagsResult getDescribeTagsResult() {
    return result;
  }

  public void setDescribeTagsResult(final DescribeTagsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
