/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ListTagsForResourceResponseType extends RdsMessage {

  private ListTagsForResourceResult result = new ListTagsForResourceResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ListTagsForResourceResult getListTagsForResourceResult() {
    return result;
  }

  public void setListTagsForResourceResult(final ListTagsForResourceResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
