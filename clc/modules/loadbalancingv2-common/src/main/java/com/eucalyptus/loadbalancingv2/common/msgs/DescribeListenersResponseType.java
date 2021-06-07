/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DescribeListenersResponseType extends Loadbalancingv2Message {

  private DescribeListenersResult result = new DescribeListenersResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DescribeListenersResult getDescribeListenersResult() {
    return result;
  }

  public void setDescribeListenersResult(final DescribeListenersResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
