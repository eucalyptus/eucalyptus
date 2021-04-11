/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class DeleteDBInstanceType extends RdsMessage {

  @Nonnull
  private String dBInstanceIdentifier;

  private Boolean deleteAutomatedBackups;

  private String finalDBSnapshotIdentifier;

  private Boolean skipFinalSnapshot;

  public String getDBInstanceIdentifier() {
    return dBInstanceIdentifier;
  }

  public void setDBInstanceIdentifier(final String dBInstanceIdentifier) {
    this.dBInstanceIdentifier = dBInstanceIdentifier;
  }

  public Boolean getDeleteAutomatedBackups() {
    return deleteAutomatedBackups;
  }

  public void setDeleteAutomatedBackups(final Boolean deleteAutomatedBackups) {
    this.deleteAutomatedBackups = deleteAutomatedBackups;
  }

  public String getFinalDBSnapshotIdentifier() {
    return finalDBSnapshotIdentifier;
  }

  public void setFinalDBSnapshotIdentifier(final String finalDBSnapshotIdentifier) {
    this.finalDBSnapshotIdentifier = finalDBSnapshotIdentifier;
  }

  public Boolean getSkipFinalSnapshot() {
    return skipFinalSnapshot;
  }

  public void setSkipFinalSnapshot(final Boolean skipFinalSnapshot) {
    this.skipFinalSnapshot = skipFinalSnapshot;
  }

}
