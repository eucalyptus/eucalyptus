/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBInstanceAutomatedBackupsResult extends EucalyptusData {

  private DBInstanceAutomatedBackupList dBInstanceAutomatedBackups;

  private String marker;

  public DBInstanceAutomatedBackupList getDBInstanceAutomatedBackups() {
    return dBInstanceAutomatedBackups;
  }

  public void setDBInstanceAutomatedBackups(final DBInstanceAutomatedBackupList dBInstanceAutomatedBackups) {
    this.dBInstanceAutomatedBackups = dBInstanceAutomatedBackups;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
