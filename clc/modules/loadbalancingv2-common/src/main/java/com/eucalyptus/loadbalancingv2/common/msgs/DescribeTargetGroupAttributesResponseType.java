/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DescribeTargetGroupAttributesResponseType extends Loadbalancingv2Message {

  private DescribeTargetGroupAttributesResult result = new DescribeTargetGroupAttributesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeTargetGroupAttributesResult getDescribeTargetGroupAttributesResult() {
    return result;
  }

  public void setDescribeTargetGroupAttributesResult(final DescribeTargetGroupAttributesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
