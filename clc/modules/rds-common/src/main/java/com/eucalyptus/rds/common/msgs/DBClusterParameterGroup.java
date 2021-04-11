/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBClusterParameterGroup extends EucalyptusData {

  private String dBClusterParameterGroupArn;

  private String dBClusterParameterGroupName;

  private String dBParameterGroupFamily;

  private String description;

  public String getDBClusterParameterGroupArn() {
    return dBClusterParameterGroupArn;
  }

  public void setDBClusterParameterGroupArn(final String dBClusterParameterGroupArn) {
    this.dBClusterParameterGroupArn = dBClusterParameterGroupArn;
  }

  public String getDBClusterParameterGroupName() {
    return dBClusterParameterGroupName;
  }

  public void setDBClusterParameterGroupName(final String dBClusterParameterGroupName) {
    this.dBClusterParameterGroupName = dBClusterParameterGroupName;
  }

  public String getDBParameterGroupFamily() {
    return dBParameterGroupFamily;
  }

  public void setDBParameterGroupFamily(final String dBParameterGroupFamily) {
    this.dBParameterGroupFamily = dBParameterGroupFamily;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

}
