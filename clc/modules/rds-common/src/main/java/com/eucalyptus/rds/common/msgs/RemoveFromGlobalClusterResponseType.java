/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class RemoveFromGlobalClusterResponseType extends RdsMessage {

  private RemoveFromGlobalClusterResult result = new RemoveFromGlobalClusterResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public RemoveFromGlobalClusterResult getRemoveFromGlobalClusterResult() {
    return result;
  }

  public void setRemoveFromGlobalClusterResult(final RemoveFromGlobalClusterResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
