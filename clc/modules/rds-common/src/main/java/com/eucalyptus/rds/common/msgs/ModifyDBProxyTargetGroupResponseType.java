/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyDBProxyTargetGroupResponseType extends RdsMessage {

  private ModifyDBProxyTargetGroupResult result = new ModifyDBProxyTargetGroupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyDBProxyTargetGroupResult getModifyDBProxyTargetGroupResult() {
    return result;
  }

  public void setModifyDBProxyTargetGroupResult(final ModifyDBProxyTargetGroupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
