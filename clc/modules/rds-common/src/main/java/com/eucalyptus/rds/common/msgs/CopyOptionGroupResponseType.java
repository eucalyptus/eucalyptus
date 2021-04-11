/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CopyOptionGroupResponseType extends RdsMessage {

  private CopyOptionGroupResult result = new CopyOptionGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CopyOptionGroupResult getCopyOptionGroupResult() {
    return result;
  }

  public void setCopyOptionGroupResult(final CopyOptionGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
