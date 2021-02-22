/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBLogFilesDetails extends EucalyptusData {

  private Long lastWritten;

  private String logFileName;

  private Long size;

  public Long getLastWritten() {
    return lastWritten;
  }

  public void setLastWritten(final Long lastWritten) {
    this.lastWritten = lastWritten;
  }

  public String getLogFileName() {
    return logFileName;
  }

  public void setLogFileName(final String logFileName) {
    this.logFileName = logFileName;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(final Long size) {
    this.size = size;
  }

}
