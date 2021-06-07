/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DeregisterDBProxyTargetsResponseType extends RdsMessage {

  private DeregisterDBProxyTargetsResult result = new DeregisterDBProxyTargetsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeregisterDBProxyTargetsResult getDeregisterDBProxyTargetsResult() {
    return result;
  }

  public void setDeregisterDBProxyTargetsResult(final DeregisterDBProxyTargetsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
