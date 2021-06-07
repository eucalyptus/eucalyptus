/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DBSnapshotAttributesResult extends EucalyptusData {

  private DBSnapshotAttributeList dBSnapshotAttributes;

  private String dBSnapshotIdentifier;

  public DBSnapshotAttributeList getDBSnapshotAttributes() {
    return dBSnapshotAttributes;
  }

  public void setDBSnapshotAttributes(final DBSnapshotAttributeList dBSnapshotAttributes) {
    this.dBSnapshotAttributes = dBSnapshotAttributes;
  }

  public String getDBSnapshotIdentifier() {
    return dBSnapshotIdentifier;
  }

  public void setDBSnapshotIdentifier(final String dBSnapshotIdentifier) {
    this.dBSnapshotIdentifier = dBSnapshotIdentifier;
  }

}
