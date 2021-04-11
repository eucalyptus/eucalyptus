/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class RemoveSourceIdentifierFromSubscriptionResponseType extends RdsMessage {

  private RemoveSourceIdentifierFromSubscriptionResult result = new RemoveSourceIdentifierFromSubscriptionResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public RemoveSourceIdentifierFromSubscriptionResult getRemoveSourceIdentifierFromSubscriptionResult() {
    return result;
  }

  public void setRemoveSourceIdentifierFromSubscriptionResult(final RemoveSourceIdentifierFromSubscriptionResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
