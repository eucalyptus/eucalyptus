/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyDBProxyResponseType extends RdsMessage {

  private ModifyDBProxyResult result = new ModifyDBProxyResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyDBProxyResult getModifyDBProxyResult() {
    return result;
  }

  public void setModifyDBProxyResult(final ModifyDBProxyResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
