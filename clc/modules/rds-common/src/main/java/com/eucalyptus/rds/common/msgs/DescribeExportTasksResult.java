/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeExportTasksResult extends EucalyptusData {

  private ExportTasksList exportTasks;

  private String marker;

  public ExportTasksList getExportTasks() {
    return exportTasks;
  }

  public void setExportTasks(final ExportTasksList exportTasks) {
    this.exportTasks = exportTasks;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
