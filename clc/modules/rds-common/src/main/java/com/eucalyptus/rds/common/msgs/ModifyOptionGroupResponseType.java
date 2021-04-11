/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyOptionGroupResponseType extends RdsMessage {

  private ModifyOptionGroupResult result = new ModifyOptionGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyOptionGroupResult getModifyOptionGroupResult() {
    return result;
  }

  public void setModifyOptionGroupResult(final ModifyOptionGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
