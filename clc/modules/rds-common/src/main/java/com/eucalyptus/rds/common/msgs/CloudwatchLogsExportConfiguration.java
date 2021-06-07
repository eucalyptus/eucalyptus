/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CloudwatchLogsExportConfiguration extends EucalyptusData {

  private LogTypeList disableLogTypes;

  private LogTypeList enableLogTypes;

  public LogTypeList getDisableLogTypes() {
    return disableLogTypes;
  }

  public void setDisableLogTypes(final LogTypeList disableLogTypes) {
    this.disableLogTypes = disableLogTypes;
  }

  public LogTypeList getEnableLogTypes() {
    return enableLogTypes;
  }

  public void setEnableLogTypes(final LogTypeList enableLogTypes) {
    this.enableLogTypes = enableLogTypes;
  }

}
