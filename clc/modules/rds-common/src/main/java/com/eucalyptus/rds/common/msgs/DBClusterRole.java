/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBClusterRole extends EucalyptusData {

  private String featureName;

  private String roleArn;

  private String status;

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

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

}
