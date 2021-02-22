/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ResourcePendingMaintenanceActions extends EucalyptusData {

  private PendingMaintenanceActionDetails pendingMaintenanceActionDetails;

  private String resourceIdentifier;

  public PendingMaintenanceActionDetails getPendingMaintenanceActionDetails() {
    return pendingMaintenanceActionDetails;
  }

  public void setPendingMaintenanceActionDetails(final PendingMaintenanceActionDetails pendingMaintenanceActionDetails) {
    this.pendingMaintenanceActionDetails = pendingMaintenanceActionDetails;
  }

  public String getResourceIdentifier() {
    return resourceIdentifier;
  }

  public void setResourceIdentifier(final String resourceIdentifier) {
    this.resourceIdentifier = resourceIdentifier;
  }

}
