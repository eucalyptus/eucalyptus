/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
