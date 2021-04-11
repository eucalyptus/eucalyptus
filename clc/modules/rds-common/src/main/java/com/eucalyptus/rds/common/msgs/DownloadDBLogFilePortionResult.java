/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DownloadDBLogFilePortionResult extends EucalyptusData {

  private Boolean additionalDataPending;

  private String logFileData;

  private String marker;

  public Boolean getAdditionalDataPending() {
    return additionalDataPending;
  }

  public void setAdditionalDataPending(final Boolean additionalDataPending) {
    this.additionalDataPending = additionalDataPending;
  }

  public String getLogFileData() {
    return logFileData;
  }

  public void setLogFileData(final String logFileData) {
    this.logFileData = logFileData;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
