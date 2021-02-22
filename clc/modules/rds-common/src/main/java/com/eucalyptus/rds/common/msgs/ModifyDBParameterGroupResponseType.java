/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyDBParameterGroupResponseType extends RdsMessage {

  private ModifyDBParameterGroupResult result = new ModifyDBParameterGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyDBParameterGroupResult getModifyDBParameterGroupResult() {
    return result;
  }

  public void setModifyDBParameterGroupResult(final ModifyDBParameterGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
