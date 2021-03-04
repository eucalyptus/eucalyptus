/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

public class AddTagsResponseType extends Loadbalancingv2Message {

  private AddTagsResult result = new AddTagsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public AddTagsResult getAddTagsResult() {
    return result;
  }

  public void setAddTagsResult(final AddTagsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
