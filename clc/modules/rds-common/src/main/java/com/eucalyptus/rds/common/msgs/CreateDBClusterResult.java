/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CreateDBClusterResult extends EucalyptusData {

  private DBCluster dBCluster;

  public DBCluster getDBCluster() {
    return dBCluster;
  }

  public void setDBCluster(final DBCluster dBCluster) {
    this.dBCluster = dBCluster;
  }

}
