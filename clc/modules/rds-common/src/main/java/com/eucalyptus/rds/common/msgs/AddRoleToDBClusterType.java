/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class AddRoleToDBClusterType extends RdsMessage {

  @Nonnull
  private String dBClusterIdentifier;

  private String featureName;

  @Nonnull
  private String roleArn;

  public String getDBClusterIdentifier() {
    return dBClusterIdentifier;
  }

  public void setDBClusterIdentifier(final String dBClusterIdentifier) {
    this.dBClusterIdentifier = dBClusterIdentifier;
  }

  public String getFeatureName() {
    return featureName;
  }

  public void setFeatureName(final String featureName) {
    this.featureName = featureName;
  }

  public String getRoleArn() {
    return roleArn;
  }

  public void setRoleArn(final String roleArn) {
    this.roleArn = roleArn;
  }

}
