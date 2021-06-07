/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ModifyDBInstanceResult extends EucalyptusData {

  private DBInstance dBInstance;

  public DBInstance getDBInstance() {
    return dBInstance;
  }

  public void setDBInstance(final DBInstance dBInstance) {
    this.dBInstance = dBInstance;
  }

}
