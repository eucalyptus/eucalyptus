/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class RevokeDBSecurityGroupIngressResponseType extends RdsMessage {

  private RevokeDBSecurityGroupIngressResult result = new RevokeDBSecurityGroupIngressResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

  public RevokeDBSecurityGroupIngressResult getRevokeDBSecurityGroupIngressResult() {
    return result;
  }

  public void setRevokeDBSecurityGroupIngressResult(final RevokeDBSecurityGroupIngressResult result) {
    this.result = result;
  }

}
