/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class AddSourceIdentifierToSubscriptionResponseType extends RdsMessage {

  private AddSourceIdentifierToSubscriptionResult result = new AddSourceIdentifierToSubscriptionResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public AddSourceIdentifierToSubscriptionResult getAddSourceIdentifierToSubscriptionResult() {
    return result;
  }

  public void setAddSourceIdentifierToSubscriptionResult(final AddSourceIdentifierToSubscriptionResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
