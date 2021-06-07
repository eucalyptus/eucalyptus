/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CopyDBParameterGroupResponseType extends RdsMessage {

  private CopyDBParameterGroupResult result = new CopyDBParameterGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CopyDBParameterGroupResult getCopyDBParameterGroupResult() {
    return result;
  }

  public void setCopyDBParameterGroupResult(final CopyDBParameterGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
