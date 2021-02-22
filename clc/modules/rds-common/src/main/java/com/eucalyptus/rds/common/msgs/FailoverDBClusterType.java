/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class FailoverDBClusterType extends RdsMessage {

  @Nonnull
  private String dBClusterIdentifier;

  private String targetDBInstanceIdentifier;

  public String getDBClusterIdentifier() {
    return dBClusterIdentifier;
  }

  public void setDBClusterIdentifier(final String dBClusterIdentifier) {
    this.dBClusterIdentifier = dBClusterIdentifier;
  }

  public String getTargetDBInstanceIdentifier() {
    return targetDBInstanceIdentifier;
  }

  public void setTargetDBInstanceIdentifier(final String targetDBInstanceIdentifier) {
    this.targetDBInstanceIdentifier = targetDBInstanceIdentifier;
  }

}
