/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DescribeTargetHealthResponseType extends Loadbalancingv2Message {

  private DescribeTargetHealthResult result = new DescribeTargetHealthResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeTargetHealthResult getDescribeTargetHealthResult() {
    return result;
  }

  public void setDescribeTargetHealthResult(final DescribeTargetHealthResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
