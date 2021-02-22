/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ModifyDBSnapshotAttributeResult extends EucalyptusData {

  private DBSnapshotAttributesResult dBSnapshotAttributesResult;

  public DBSnapshotAttributesResult getDBSnapshotAttributesResult() {
    return dBSnapshotAttributesResult;
  }

  public void setDBSnapshotAttributesResult(final DBSnapshotAttributesResult dBSnapshotAttributesResult) {
    this.dBSnapshotAttributesResult = dBSnapshotAttributesResult;
  }

}
