/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DescribeRulesResponseType extends Loadbalancingv2Message {

  private DescribeRulesResult result = new DescribeRulesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeRulesResult getDescribeRulesResult() {
    return result;
  }

  public void setDescribeRulesResult(final DescribeRulesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
