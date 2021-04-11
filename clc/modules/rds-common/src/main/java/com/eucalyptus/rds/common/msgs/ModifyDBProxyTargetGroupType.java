/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class ModifyDBProxyTargetGroupType extends RdsMessage {

  private ConnectionPoolConfiguration connectionPoolConfig;

  @Nonnull
  private String dBProxyName;

  private String newName;

  @Nonnull
  private String targetGroupName;

  public ConnectionPoolConfiguration getConnectionPoolConfig() {
    return connectionPoolConfig;
  }

  public void setConnectionPoolConfig(final ConnectionPoolConfiguration connectionPoolConfig) {
    this.connectionPoolConfig = connectionPoolConfig;
  }

  public String getDBProxyName() {
    return dBProxyName;
  }

  public void setDBProxyName(final String dBProxyName) {
    this.dBProxyName = dBProxyName;
  }

  public String getNewName() {
    return newName;
  }

  public void setNewName(final String newName) {
    this.newName = newName;
  }

  public String getTargetGroupName() {
    return targetGroupName;
  }

  public void setTargetGroupName(final String targetGroupName) {
    this.targetGroupName = targetGroupName;
  }

}
