/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CreateDBProxyResult extends EucalyptusData {

  private DBProxy dBProxy;

  public DBProxy getDBProxy() {
    return dBProxy;
  }

  public void setDBProxy(final DBProxy dBProxy) {
    this.dBProxy = dBProxy;
  }

}
