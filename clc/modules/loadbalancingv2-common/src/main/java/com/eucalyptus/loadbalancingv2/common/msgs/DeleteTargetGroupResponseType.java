/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class DeleteTargetGroupResponseType extends Loadbalancingv2Message {

  private DeleteTargetGroupResult result = new DeleteTargetGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeleteTargetGroupResult getDeleteTargetGroupResult() {
    return result;
  }

  public void setDeleteTargetGroupResult(final DeleteTargetGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
