/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DeleteListenerResponseType extends Loadbalancingv2Message {

  private DeleteListenerResult result = new DeleteListenerResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeleteListenerResult getDeleteListenerResult() {
    return result;
  }

  public void setDeleteListenerResult(final DeleteListenerResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
