/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DeleteDBProxyResponseType extends RdsMessage {

  private DeleteDBProxyResult result = new DeleteDBProxyResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeleteDBProxyResult getDeleteDBProxyResult() {
    return result;
  }

  public void setDeleteDBProxyResult(final DeleteDBProxyResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
