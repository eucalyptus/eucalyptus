/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ModifyDBProxyTargetGroupResult extends EucalyptusData {

  private DBProxyTargetGroup dBProxyTargetGroup;

  public DBProxyTargetGroup getDBProxyTargetGroup() {
    return dBProxyTargetGroup;
  }

  public void setDBProxyTargetGroup(final DBProxyTargetGroup dBProxyTargetGroup) {
    this.dBProxyTargetGroup = dBProxyTargetGroup;
  }

}
