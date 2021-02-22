/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ModifyDBSubnetGroupResult extends EucalyptusData {

  private DBSubnetGroup dBSubnetGroup;

  public DBSubnetGroup getDBSubnetGroup() {
    return dBSubnetGroup;
  }

  public void setDBSubnetGroup(final DBSubnetGroup dBSubnetGroup) {
    this.dBSubnetGroup = dBSubnetGroup;
  }

}
