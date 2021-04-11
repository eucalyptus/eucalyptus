/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class AddRoleToDBInstanceType extends RdsMessage {

  @Nonnull
  private String dBInstanceIdentifier;

  @Nonnull
  private String featureName;

  @Nonnull
  private String roleArn;

  public String getDBInstanceIdentifier() {
    return dBInstanceIdentifier;
  }

  public void setDBInstanceIdentifier(final String dBInstanceIdentifier) {
    this.dBInstanceIdentifier = dBInstanceIdentifier;
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
