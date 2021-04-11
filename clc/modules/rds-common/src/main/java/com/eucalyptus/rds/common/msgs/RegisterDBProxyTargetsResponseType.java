/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class RegisterDBProxyTargetsResponseType extends RdsMessage {

  private RegisterDBProxyTargetsResult result = new RegisterDBProxyTargetsResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public RegisterDBProxyTargetsResult getRegisterDBProxyTargetsResult() {
    return result;
  }

  public void setRegisterDBProxyTargetsResult(final RegisterDBProxyTargetsResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
