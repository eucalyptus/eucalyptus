/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class CreateTargetGroupResponseType extends Loadbalancingv2Message {

  private CreateTargetGroupResult result = new CreateTargetGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateTargetGroupResult getCreateTargetGroupResult() {
    return result;
  }

  public void setCreateTargetGroupResult(final CreateTargetGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
