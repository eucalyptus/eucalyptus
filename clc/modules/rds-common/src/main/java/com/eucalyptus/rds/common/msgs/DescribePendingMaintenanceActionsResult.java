/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribePendingMaintenanceActionsResult extends EucalyptusData {

  private String marker;

  private PendingMaintenanceActions pendingMaintenanceActions;

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public PendingMaintenanceActions getPendingMaintenanceActions() {
    return pendingMaintenanceActions;
  }

  public void setPendingMaintenanceActions(final PendingMaintenanceActions pendingMaintenanceActions) {
    this.pendingMaintenanceActions = pendingMaintenanceActions;
  }

}
