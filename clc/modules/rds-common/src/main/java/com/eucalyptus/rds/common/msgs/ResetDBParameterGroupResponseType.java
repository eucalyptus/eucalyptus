/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ResetDBParameterGroupResponseType extends RdsMessage {

  private ResetDBParameterGroupResult result = new ResetDBParameterGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResetDBParameterGroupResult getResetDBParameterGroupResult() {
    return result;
  }

  public void setResetDBParameterGroupResult(final ResetDBParameterGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
