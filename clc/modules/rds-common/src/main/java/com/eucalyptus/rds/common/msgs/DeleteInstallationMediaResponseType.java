/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DeleteInstallationMediaResponseType extends RdsMessage {

  private DeleteInstallationMediaResult result = new DeleteInstallationMediaResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeleteInstallationMediaResult getDeleteInstallationMediaResult() {
    return result;
  }

  public void setDeleteInstallationMediaResult(final DeleteInstallationMediaResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
