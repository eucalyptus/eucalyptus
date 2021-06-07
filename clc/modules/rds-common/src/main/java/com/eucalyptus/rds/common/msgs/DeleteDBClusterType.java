/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class DeleteDBClusterType extends RdsMessage {

  @Nonnull
  private String dBClusterIdentifier;

  private String finalDBSnapshotIdentifier;

  private Boolean skipFinalSnapshot;

  public String getDBClusterIdentifier() {
    return dBClusterIdentifier;
  }

  public void setDBClusterIdentifier(final String dBClusterIdentifier) {
    this.dBClusterIdentifier = dBClusterIdentifier;
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
