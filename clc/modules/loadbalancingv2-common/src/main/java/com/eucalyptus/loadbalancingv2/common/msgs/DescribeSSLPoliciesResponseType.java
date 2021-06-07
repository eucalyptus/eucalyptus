/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DescribeSSLPoliciesResponseType extends Loadbalancingv2Message {

  private DescribeSSLPoliciesResult result = new DescribeSSLPoliciesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeSSLPoliciesResult getDescribeSSLPoliciesResult() {
    return result;
  }

  public void setDescribeSSLPoliciesResult(final DescribeSSLPoliciesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
