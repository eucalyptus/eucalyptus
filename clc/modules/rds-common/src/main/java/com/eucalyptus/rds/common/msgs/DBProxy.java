/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBProxy extends EucalyptusData {

  private UserAuthConfigInfoList auth;

  private java.util.Date createdDate;

  private String dBProxyArn;

  private String dBProxyName;

  private Boolean debugLogging;

  private String endpoint;

  private String engineFamily;

  private Integer idleClientTimeout;

  private Boolean requireTLS;

  private String roleArn;

  @FieldRegex(FieldRegexValue.ENUM_DBPROXYSTATUS)
  private String status;

  private java.util.Date updatedDate;

  private StringList vpcSecurityGroupIds;

  private StringList vpcSubnetIds;

  public UserAuthConfigInfoList getAuth() {
    return auth;
  }

  public void setAuth(final UserAuthConfigInfoList auth) {
    this.auth = auth;
  }

  public java.util.Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(final java.util.Date createdDate) {
    this.createdDate = createdDate;
  }

  public String getDBProxyArn() {
    return dBProxyArn;
  }

  public void setDBProxyArn(final String dBProxyArn) {
    this.dBProxyArn = dBProxyArn;
  }

  public String getDBProxyName() {
    return dBProxyName;
  }

  public void setDBProxyName(final String dBProxyName) {
    this.dBProxyName = dBProxyName;
  }

  public Boolean getDebugLogging() {
    return debugLogging;
  }

  public void setDebugLogging(final Boolean debugLogging) {
    this.debugLogging = debugLogging;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(final String endpoint) {
    this.endpoint = endpoint;
  }

  public String getEngineFamily() {
    return engineFamily;
  }

  public void setEngineFamily(final String engineFamily) {
    this.engineFamily = engineFamily;
  }

  public Integer getIdleClientTimeout() {
    return idleClientTimeout;
  }

  public void setIdleClientTimeout(final Integer idleClientTimeout) {
    this.idleClientTimeout = idleClientTimeout;
  }

  public Boolean getRequireTLS() {
    return requireTLS;
  }

  public void setRequireTLS(final Boolean requireTLS) {
    this.requireTLS = requireTLS;
  }

  public String getRoleArn() {
    return roleArn;
  }

  public void setRoleArn(final String roleArn) {
    this.roleArn = roleArn;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public java.util.Date getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(final java.util.Date updatedDate) {
    this.updatedDate = updatedDate;
  }

  public StringList getVpcSecurityGroupIds() {
    return vpcSecurityGroupIds;
  }

  public void setVpcSecurityGroupIds(final StringList vpcSecurityGroupIds) {
    this.vpcSecurityGroupIds = vpcSecurityGroupIds;
  }

  public StringList getVpcSubnetIds() {
    return vpcSubnetIds;
  }

  public void setVpcSubnetIds(final StringList vpcSubnetIds) {
    this.vpcSubnetIds = vpcSubnetIds;
  }

}
