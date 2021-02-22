/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBParameterGroup extends EucalyptusData {

  private String dBParameterGroupArn;

  private String dBParameterGroupFamily;

  private String dBParameterGroupName;

  private String description;

  public String getDBParameterGroupArn() {
    return dBParameterGroupArn;
  }

  public void setDBParameterGroupArn(final String dBParameterGroupArn) {
    this.dBParameterGroupArn = dBParameterGroupArn;
  }

  public String getDBParameterGroupFamily() {
    return dBParameterGroupFamily;
  }

  public void setDBParameterGroupFamily(final String dBParameterGroupFamily) {
    this.dBParameterGroupFamily = dBParameterGroupFamily;
  }

  public String getDBParameterGroupName() {
    return dBParameterGroupName;
  }

  public void setDBParameterGroupName(final String dBParameterGroupName) {
    this.dBParameterGroupName = dBParameterGroupName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

}
