/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class PromoteReadReplicaResponseType extends RdsMessage {

  private PromoteReadReplicaResult result = new PromoteReadReplicaResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public PromoteReadReplicaResult getPromoteReadReplicaResult() {
    return result;
  }

  public void setPromoteReadReplicaResult(final PromoteReadReplicaResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
