/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class CreateListenerResponseType extends Loadbalancingv2Message {

  private CreateListenerResult result = new CreateListenerResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateListenerResult getCreateListenerResult() {
    return result;
  }

  public void setCreateListenerResult(final CreateListenerResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
