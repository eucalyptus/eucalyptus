/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class ModifyLoadBalancerAttributesResponseType extends Loadbalancingv2Message {

  private ModifyLoadBalancerAttributesResult result = new ModifyLoadBalancerAttributesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyLoadBalancerAttributesResult getModifyLoadBalancerAttributesResult() {
    return result;
  }

  public void setModifyLoadBalancerAttributesResult(final ModifyLoadBalancerAttributesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
