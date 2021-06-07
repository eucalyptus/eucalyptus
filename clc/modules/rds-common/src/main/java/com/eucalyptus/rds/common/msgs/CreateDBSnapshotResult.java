/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CreateDBSnapshotResult extends EucalyptusData {

  private DBSnapshot dBSnapshot;

  public DBSnapshot getDBSnapshot() {
    return dBSnapshot;
  }

  public void setDBSnapshot(final DBSnapshot dBSnapshot) {
    this.dBSnapshot = dBSnapshot;
  }

}
