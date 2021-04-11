/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CreateDBClusterSnapshotResult extends EucalyptusData {

  private DBClusterSnapshot dBClusterSnapshot;

  public DBClusterSnapshot getDBClusterSnapshot() {
    return dBClusterSnapshot;
  }

  public void setDBClusterSnapshot(final DBClusterSnapshot dBClusterSnapshot) {
    this.dBClusterSnapshot = dBClusterSnapshot;
  }

}
