/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyDBClusterParameterGroupResponseType extends RdsMessage {

  private ModifyDBClusterParameterGroupResult result = new ModifyDBClusterParameterGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyDBClusterParameterGroupResult getModifyDBClusterParameterGroupResult() {
    return result;
  }

  public void setModifyDBClusterParameterGroupResult(final ModifyDBClusterParameterGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
