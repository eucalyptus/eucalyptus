/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DescribeTargetGroupsResponseType extends Loadbalancingv2Message {

  private DescribeTargetGroupsResult result = new DescribeTargetGroupsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeTargetGroupsResult getDescribeTargetGroupsResult() {
    return result;
  }

  public void setDescribeTargetGroupsResult(final DescribeTargetGroupsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
