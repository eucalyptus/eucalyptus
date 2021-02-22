/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class AuthorizeDBSecurityGroupIngressResponseType extends RdsMessage {

  private AuthorizeDBSecurityGroupIngressResult result = new AuthorizeDBSecurityGroupIngressResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public AuthorizeDBSecurityGroupIngressResult getAuthorizeDBSecurityGroupIngressResult() {
    return result;
  }

  public void setAuthorizeDBSecurityGroupIngressResult(final AuthorizeDBSecurityGroupIngressResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
