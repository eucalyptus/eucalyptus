/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class RemoveTagsResponseType extends Loadbalancingv2Message {

  private RemoveTagsResult result = new RemoveTagsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public RemoveTagsResult getRemoveTagsResult() {
    return result;
  }

  public void setRemoveTagsResult(final RemoveTagsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
