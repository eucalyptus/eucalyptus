/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBProxyTargetGroup extends EucalyptusData {

  private ConnectionPoolConfigurationInfo connectionPoolConfig;

  private java.util.Date createdDate;

  private String dBProxyName;

  private Boolean isDefault;

  private String status;

  private String targetGroupArn;

  private String targetGroupName;

  private java.util.Date updatedDate;

  public ConnectionPoolConfigurationInfo getConnectionPoolConfig() {
    return connectionPoolConfig;
  }

  public void setConnectionPoolConfig(final ConnectionPoolConfigurationInfo connectionPoolConfig) {
    this.connectionPoolConfig = connectionPoolConfig;
  }

  public java.util.Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(final java.util.Date createdDate) {
    this.createdDate = createdDate;
  }

  public String getDBProxyName() {
    return dBProxyName;
  }

  public void setDBProxyName(final String dBProxyName) {
    this.dBProxyName = dBProxyName;
  }

  public Boolean getIsDefault() {
    return isDefault;
  }

  public void setIsDefault(final Boolean isDefault) {
    this.isDefault = isDefault;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public String getTargetGroupArn() {
    return targetGroupArn;
  }

  public void setTargetGroupArn(final String targetGroupArn) {
    this.targetGroupArn = targetGroupArn;
  }

  public String getTargetGroupName() {
    return targetGroupName;
  }

  public void setTargetGroupName(final String targetGroupName) {
    this.targetGroupName = targetGroupName;
  }

  public java.util.Date getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(final java.util.Date updatedDate) {
    this.updatedDate = updatedDate;
  }

}
