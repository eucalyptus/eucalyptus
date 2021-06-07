/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DeleteDBInstanceAutomatedBackupResult extends EucalyptusData {

  private DBInstanceAutomatedBackup dBInstanceAutomatedBackup;

  public DBInstanceAutomatedBackup getDBInstanceAutomatedBackup() {
    return dBInstanceAutomatedBackup;
  }

  public void setDBInstanceAutomatedBackup(final DBInstanceAutomatedBackup dBInstanceAutomatedBackup) {
    this.dBInstanceAutomatedBackup = dBInstanceAutomatedBackup;
  }

}
