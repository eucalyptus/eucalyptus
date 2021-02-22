/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBParameterGroupStatus extends EucalyptusData {

  private String dBParameterGroupName;

  private String parameterApplyStatus;

  public String getDBParameterGroupName() {
    return dBParameterGroupName;
  }

  public void setDBParameterGroupName(final String dBParameterGroupName) {
    this.dBParameterGroupName = dBParameterGroupName;
  }

  public String getParameterApplyStatus() {
    return parameterApplyStatus;
  }

  public void setParameterApplyStatus(final String parameterApplyStatus) {
    this.parameterApplyStatus = parameterApplyStatus;
  }

}
