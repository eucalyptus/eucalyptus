/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;


public class CreateDBProxyType extends RdsMessage {

  @Nonnull
  private UserAuthConfigList auth;

  @Nonnull
  private String dBProxyName;

  private Boolean debugLogging;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_ENGINEFAMILY)
  private String engineFamily;

  private Integer idleClientTimeout;

  private Boolean requireTLS;

  @Nonnull
  private String roleArn;

  private TagList tags;

  private StringList vpcSecurityGroupIds;

  @Nonnull
  private StringList vpcSubnetIds;

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

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
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
