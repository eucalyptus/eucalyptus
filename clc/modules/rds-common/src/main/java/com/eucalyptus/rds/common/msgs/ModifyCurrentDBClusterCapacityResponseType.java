/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyCurrentDBClusterCapacityResponseType extends RdsMessage {

  private ModifyCurrentDBClusterCapacityResult result = new ModifyCurrentDBClusterCapacityResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyCurrentDBClusterCapacityResult getModifyCurrentDBClusterCapacityResult() {
    return result;
  }

  public void setModifyCurrentDBClusterCapacityResult(final ModifyCurrentDBClusterCapacityResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
