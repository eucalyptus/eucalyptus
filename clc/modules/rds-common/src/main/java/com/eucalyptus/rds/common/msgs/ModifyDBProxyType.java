/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class ModifyDBProxyType extends RdsMessage {

  private UserAuthConfigList auth;

  @Nonnull
  private String dBProxyName;

  private Boolean debugLogging;

  private Integer idleClientTimeout;

  private String newDBProxyName;

  private Boolean requireTLS;

  private String roleArn;

  private StringList securityGroups;

  public UserAuthConfigList getAuth() {
    return auth;
  }

  public void setAuth(final UserAuthConfigList auth) {
    this.auth = auth;
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

  public Integer getIdleClientTimeout() {
    return idleClientTimeout;
  }

  public void setIdleClientTimeout(final Integer idleClientTimeout) {
    this.idleClientTimeout = idleClientTimeout;
  }

  public String getNewDBProxyName() {
    return newDBProxyName;
  }

  public void setNewDBProxyName(final String newDBProxyName) {
    this.newDBProxyName = newDBProxyName;
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

  public StringList getSecurityGroups() {
    return securityGroups;
  }

  public void setSecurityGroups(final StringList securityGroups) {
    this.securityGroups = securityGroups;
  }

}
