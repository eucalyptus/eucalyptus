/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class ModifyTargetGroupAttributesResponseType extends Loadbalancingv2Message {

  private ModifyTargetGroupAttributesResult result = new ModifyTargetGroupAttributesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyTargetGroupAttributesResult getModifyTargetGroupAttributesResult() {
    return result;
  }

  public void setModifyTargetGroupAttributesResult(final ModifyTargetGroupAttributesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
