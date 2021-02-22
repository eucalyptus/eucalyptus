/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBClusterOptionGroupStatus extends EucalyptusData {

  private String dBClusterOptionGroupName;

  private String status;

  public String getDBClusterOptionGroupName() {
    return dBClusterOptionGroupName;
  }

  public void setDBClusterOptionGroupName(final String dBClusterOptionGroupName) {
    this.dBClusterOptionGroupName = dBClusterOptionGroupName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

}
