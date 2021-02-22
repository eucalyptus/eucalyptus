/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class RemoveFromGlobalClusterType extends RdsMessage {

  private String dbClusterIdentifier;

  private String globalClusterIdentifier;

  public String getDbClusterIdentifier() {
    return dbClusterIdentifier;
  }

  public void setDbClusterIdentifier(final String dbClusterIdentifier) {
    this.dbClusterIdentifier = dbClusterIdentifier;
  }

  public String getGlobalClusterIdentifier() {
    return globalClusterIdentifier;
  }

  public void setGlobalClusterIdentifier(final String globalClusterIdentifier) {
    this.globalClusterIdentifier = globalClusterIdentifier;
  }

}
