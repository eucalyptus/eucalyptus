/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ModifyCertificatesResponseType extends RdsMessage {

  private ModifyCertificatesResult result = new ModifyCertificatesResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ModifyCertificatesResult getModifyCertificatesResult() {
    return result;
  }

  public void setModifyCertificatesResult(final ModifyCertificatesResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
