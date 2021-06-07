/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DescribeAccountLimitsResponseType extends Loadbalancingv2Message {

  private DescribeAccountLimitsResult result = new DescribeAccountLimitsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeAccountLimitsResult getDescribeAccountLimitsResult() {
    return result;
  }

  public void setDescribeAccountLimitsResult(final DescribeAccountLimitsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
