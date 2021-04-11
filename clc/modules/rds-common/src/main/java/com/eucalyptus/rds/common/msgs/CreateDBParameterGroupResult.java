/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CreateDBParameterGroupResult extends EucalyptusData {

  private DBParameterGroup dBParameterGroup;

  public DBParameterGroup getDBParameterGroup() {
    return dBParameterGroup;
  }

  public void setDBParameterGroup(final DBParameterGroup dBParameterGroup) {
    this.dBParameterGroup = dBParameterGroup;
  }

}
