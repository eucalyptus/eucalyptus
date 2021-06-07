/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DescribeLoadBalancersResponseType extends Loadbalancingv2Message {

  private DescribeLoadBalancersResult result = new DescribeLoadBalancersResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeLoadBalancersResult getDescribeLoadBalancersResult() {
    return result;
  }

  public void setDescribeLoadBalancersResult(final DescribeLoadBalancersResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
