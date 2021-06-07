/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class SetSubnetsResponseType extends Loadbalancingv2Message {

  private SetSubnetsResult result = new SetSubnetsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public SetSubnetsResult getSetSubnetsResult() {
    return result;
  }

  public void setSetSubnetsResult(final SetSubnetsResult result) {
    this.result = result;
  }

}
