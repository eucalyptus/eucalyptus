/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DescribeLoadBalancerAttributesResponseType extends Loadbalancingv2Message {

  private DescribeLoadBalancerAttributesResult result = new DescribeLoadBalancerAttributesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeLoadBalancerAttributesResult getDescribeLoadBalancerAttributesResult() {
    return result;
  }

  public void setDescribeLoadBalancerAttributesResult(final DescribeLoadBalancerAttributesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
