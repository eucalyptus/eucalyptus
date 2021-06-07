/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class DownloadDBLogFilePortionType extends RdsMessage {

  @Nonnull
  private String dBInstanceIdentifier;

  @Nonnull
  private String logFileName;

  private String marker;

  private Integer numberOfLines;

  public String getDBInstanceIdentifier() {
    return dBInstanceIdentifier;
  }

  public void setDBInstanceIdentifier(final String dBInstanceIdentifier) {
    this.dBInstanceIdentifier = dBInstanceIdentifier;
  }

  public String getLogFileName() {
    return logFileName;
  }

  public void setLogFileName(final String logFileName) {
    this.logFileName = logFileName;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public Integer getNumberOfLines() {
    return numberOfLines;
  }

  public void setNumberOfLines(final Integer numberOfLines) {
    this.numberOfLines = numberOfLines;
  }

}
