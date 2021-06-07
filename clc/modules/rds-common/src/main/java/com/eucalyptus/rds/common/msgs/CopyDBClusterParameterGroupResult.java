/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CopyDBClusterParameterGroupResult extends EucalyptusData {

  private DBClusterParameterGroup dBClusterParameterGroup;

  public DBClusterParameterGroup getDBClusterParameterGroup() {
    return dBClusterParameterGroup;
  }

  public void setDBClusterParameterGroup(final DBClusterParameterGroup dBClusterParameterGroup) {
    this.dBClusterParameterGroup = dBClusterParameterGroup;
  }

}
