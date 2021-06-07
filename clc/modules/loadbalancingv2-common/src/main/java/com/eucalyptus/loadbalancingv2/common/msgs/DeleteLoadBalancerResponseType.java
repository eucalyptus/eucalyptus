/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DeleteLoadBalancerResponseType extends Loadbalancingv2Message {

  private DeleteLoadBalancerResult result = new DeleteLoadBalancerResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeleteLoadBalancerResult getDeleteLoadBalancerResult() {
    return result;
  }

  public void setDeleteLoadBalancerResult(final DeleteLoadBalancerResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
