/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DeleteEventSubscriptionResponseType extends RdsMessage {

  private DeleteEventSubscriptionResult result = new DeleteEventSubscriptionResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeleteEventSubscriptionResult getDeleteEventSubscriptionResult() {
    return result;
  }

  public void setDeleteEventSubscriptionResult(final DeleteEventSubscriptionResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
