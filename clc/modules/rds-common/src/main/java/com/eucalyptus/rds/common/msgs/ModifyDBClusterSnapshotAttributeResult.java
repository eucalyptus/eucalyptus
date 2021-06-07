/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ModifyDBClusterSnapshotAttributeResult extends EucalyptusData {

  private DBClusterSnapshotAttributesResult dBClusterSnapshotAttributesResult;

  public DBClusterSnapshotAttributesResult getDBClusterSnapshotAttributesResult() {
    return dBClusterSnapshotAttributesResult;
  }

  public void setDBClusterSnapshotAttributesResult(final DBClusterSnapshotAttributesResult dBClusterSnapshotAttributesResult) {
    this.dBClusterSnapshotAttributesResult = dBClusterSnapshotAttributesResult;
  }

}
