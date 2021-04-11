/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ApplyPendingMaintenanceActionResult extends EucalyptusData {

  private ResourcePendingMaintenanceActions resourcePendingMaintenanceActions;

  public ResourcePendingMaintenanceActions getResourcePendingMaintenanceActions() {
    return resourcePendingMaintenanceActions;
  }

  public void setResourcePendingMaintenanceActions(final ResourcePendingMaintenanceActions resourcePendingMaintenanceActions) {
    this.resourcePendingMaintenanceActions = resourcePendingMaintenanceActions;
  }

}
