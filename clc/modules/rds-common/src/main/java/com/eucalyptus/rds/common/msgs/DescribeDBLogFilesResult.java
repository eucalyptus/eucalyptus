/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeDBLogFilesResult extends EucalyptusData {

  private DescribeDBLogFilesList describeDBLogFiles;

  private String marker;

  public DescribeDBLogFilesList getDescribeDBLogFiles() {
    return describeDBLogFiles;
  }

  public void setDescribeDBLogFiles(final DescribeDBLogFilesList describeDBLogFiles) {
    this.describeDBLogFiles = describeDBLogFiles;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
