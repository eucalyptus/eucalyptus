/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class ModifyTargetGroupResponseType extends Loadbalancingv2Message {

  private ModifyTargetGroupResult result = new ModifyTargetGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyTargetGroupResult getModifyTargetGroupResult() {
    return result;
  }

  public void setModifyTargetGroupResult(final ModifyTargetGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
