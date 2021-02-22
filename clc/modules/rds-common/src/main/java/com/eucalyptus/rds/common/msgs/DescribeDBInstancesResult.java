/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBInstancesResult extends EucalyptusData {

  private DBInstanceList dBInstances;

  private String marker;

  public DBInstanceList getDBInstances() {
    return dBInstances;
  }

  public void setDBInstances(final DBInstanceList dBInstances) {
    this.dBInstances = dBInstances;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
