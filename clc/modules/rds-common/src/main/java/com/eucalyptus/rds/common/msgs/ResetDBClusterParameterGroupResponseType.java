/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ResetDBClusterParameterGroupResponseType extends RdsMessage {

  private ResetDBClusterParameterGroupResult result = new ResetDBClusterParameterGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResetDBClusterParameterGroupResult getResetDBClusterParameterGroupResult() {
    return result;
  }

  public void setResetDBClusterParameterGroupResult(final ResetDBClusterParameterGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
