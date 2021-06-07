/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class PromoteReadReplicaDBClusterResponseType extends RdsMessage {

  private PromoteReadReplicaDBClusterResult result = new PromoteReadReplicaDBClusterResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public PromoteReadReplicaDBClusterResult getPromoteReadReplicaDBClusterResult() {
    return result;
  }

  public void setPromoteReadReplicaDBClusterResult(final PromoteReadReplicaDBClusterResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
