/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CreateOptionGroupResponseType extends RdsMessage {

  private CreateOptionGroupResult result = new CreateOptionGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CreateOptionGroupResult getCreateOptionGroupResult() {
    return result;
  }

  public void setCreateOptionGroupResult(final CreateOptionGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
