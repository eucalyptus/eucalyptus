/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpEmbedded;


public class DeleteFleetsType extends ComputeMessage {

  @HttpEmbedded
  private FleetIdSet fleetIds;
  @Nonnull
  private Boolean terminateInstances;

  public FleetIdSet getFleetIds( ) {
    return fleetIds;
  }

  public void setFleetIds( final FleetIdSet fleetIds ) {
    this.fleetIds = fleetIds;
  }

  public Boolean getTerminateInstances( ) {
    return terminateInstances;
  }

  public void setTerminateInstances( final Boolean terminateInstances ) {
    this.terminateInstances = terminateInstances;
  }

}
