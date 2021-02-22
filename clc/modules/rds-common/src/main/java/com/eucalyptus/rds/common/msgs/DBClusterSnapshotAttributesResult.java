/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBClusterSnapshotAttributesResult extends EucalyptusData {

  private DBClusterSnapshotAttributeList dBClusterSnapshotAttributes;

  private String dBClusterSnapshotIdentifier;

  public DBClusterSnapshotAttributeList getDBClusterSnapshotAttributes() {
    return dBClusterSnapshotAttributes;
  }

  public void setDBClusterSnapshotAttributes(final DBClusterSnapshotAttributeList dBClusterSnapshotAttributes) {
    this.dBClusterSnapshotAttributes = dBClusterSnapshotAttributes;
  }

  public String getDBClusterSnapshotIdentifier() {
    return dBClusterSnapshotIdentifier;
  }

  public void setDBClusterSnapshotIdentifier(final String dBClusterSnapshotIdentifier) {
    this.dBClusterSnapshotIdentifier = dBClusterSnapshotIdentifier;
  }

}
