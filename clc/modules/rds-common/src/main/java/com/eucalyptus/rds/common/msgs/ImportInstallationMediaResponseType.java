/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class ImportInstallationMediaResponseType extends RdsMessage {

  private ImportInstallationMediaResult result = new ImportInstallationMediaResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public ImportInstallationMediaResult getImportInstallationMediaResult() {
    return result;
  }

  public void setImportInstallationMediaResult(final ImportInstallationMediaResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
