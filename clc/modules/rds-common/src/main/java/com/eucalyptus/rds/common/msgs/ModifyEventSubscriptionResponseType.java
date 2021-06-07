/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyEventSubscriptionResponseType extends RdsMessage {

  private ModifyEventSubscriptionResult result = new ModifyEventSubscriptionResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyEventSubscriptionResult getModifyEventSubscriptionResult() {
    return result;
  }

  public void setModifyEventSubscriptionResult(final ModifyEventSubscriptionResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
