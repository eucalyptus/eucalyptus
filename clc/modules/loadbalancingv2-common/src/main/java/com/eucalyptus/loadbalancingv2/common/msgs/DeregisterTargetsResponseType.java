/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DeregisterTargetsResponseType extends Loadbalancingv2Message {

  private DeregisterTargetsResult result = new DeregisterTargetsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeregisterTargetsResult getDeregisterTargetsResult() {
    return result;
  }

  public void setDeregisterTargetsResult(final DeregisterTargetsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
