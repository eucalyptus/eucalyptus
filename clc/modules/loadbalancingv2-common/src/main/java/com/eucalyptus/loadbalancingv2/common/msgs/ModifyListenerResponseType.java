/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class ModifyListenerResponseType extends Loadbalancingv2Message {

  private ModifyListenerResult result = new ModifyListenerResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyListenerResult getModifyListenerResult() {
    return result;
  }

  public void setModifyListenerResult(final ModifyListenerResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
