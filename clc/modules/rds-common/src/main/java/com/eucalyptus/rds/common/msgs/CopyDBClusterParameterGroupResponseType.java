/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class CopyDBClusterParameterGroupResponseType extends RdsMessage {

  private CopyDBClusterParameterGroupResult result = new CopyDBClusterParameterGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public CopyDBClusterParameterGroupResult getCopyDBClusterParameterGroupResult() {
    return result;
  }

  public void setCopyDBClusterParameterGroupResult(final CopyDBClusterParameterGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
