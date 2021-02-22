/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateEventSubscriptionResponseType extends RdsMessage {

  private CreateEventSubscriptionResult result = new CreateEventSubscriptionResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateEventSubscriptionResult getCreateEventSubscriptionResult() {
    return result;
  }

  public void setCreateEventSubscriptionResult(final CreateEventSubscriptionResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
