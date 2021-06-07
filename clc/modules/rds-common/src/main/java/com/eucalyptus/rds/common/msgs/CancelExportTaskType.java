/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CancelExportTaskType extends RdsMessage {

  @Nonnull
  private String exportTaskIdentifier;

  public String getExportTaskIdentifier() {
    return exportTaskIdentifier;
  }

  public void setExportTaskIdentifier(final String exportTaskIdentifier) {
    this.exportTaskIdentifier = exportTaskIdentifier;
  }

}
