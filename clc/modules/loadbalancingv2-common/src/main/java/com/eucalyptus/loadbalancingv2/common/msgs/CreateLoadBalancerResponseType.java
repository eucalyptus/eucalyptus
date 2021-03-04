/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class CreateLoadBalancerResponseType extends Loadbalancingv2Message {

  private CreateLoadBalancerResult result = new CreateLoadBalancerResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateLoadBalancerResult getCreateLoadBalancerResult() {
    return result;
  }

  public void setCreateLoadBalancerResult(final CreateLoadBalancerResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
