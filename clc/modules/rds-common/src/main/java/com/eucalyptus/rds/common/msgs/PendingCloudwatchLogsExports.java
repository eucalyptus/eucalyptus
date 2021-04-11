/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class PendingCloudwatchLogsExports extends EucalyptusData {

  private LogTypeList logTypesToDisable;

  private LogTypeList logTypesToEnable;

  public LogTypeList getLogTypesToDisable() {
    return logTypesToDisable;
  }

  public void setLogTypesToDisable(final LogTypeList logTypesToDisable) {
    this.logTypesToDisable = logTypesToDisable;
  }

  public LogTypeList getLogTypesToEnable() {
    return logTypesToEnable;
  }

  public void setLogTypesToEnable(final LogTypeList logTypesToEnable) {
    this.logTypesToEnable = logTypesToEnable;
  }

}
