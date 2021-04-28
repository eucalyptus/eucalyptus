/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class RegisterTargetsResponseType extends Loadbalancingv2Message {

  private RegisterTargetsResult result = new RegisterTargetsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public RegisterTargetsResult getRegisterTargetsResult() {
    return result;
  }

  public void setRegisterTargetsResult(final RegisterTargetsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
