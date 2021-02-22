/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DeleteDBInstanceAutomatedBackupResponseType extends RdsMessage {

  private DeleteDBInstanceAutomatedBackupResult result = new DeleteDBInstanceAutomatedBackupResult();

  private ResponseMetadata responseMetadata = new ResponseMetadata();

  public DeleteDBInstanceAutomatedBackupResult getDeleteDBInstanceAutomatedBackupResult() {
    return result;
  }

  public void setDeleteDBInstanceAutomatedBackupResult(final DeleteDBInstanceAutomatedBackupResult result) {
    this.result = result;
  }

  public ResponseMetadata getResponseMetadata() {
    return responseMetadata;
  }

  public void setResponseMetadata(final ResponseMetadata responseMetadata) {
    this.responseMetadata = responseMetadata;
  }

}
