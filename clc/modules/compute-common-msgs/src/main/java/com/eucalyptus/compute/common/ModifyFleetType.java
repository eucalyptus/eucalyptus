/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class ModifyFleetType extends ComputeMessage {

  private String excessCapacityTerminationPolicy;
  @Nonnull
  private String fleetId;
  @Nonnull
  private TargetCapacitySpecificationRequest targetCapacitySpecification;

  public String getExcessCapacityTerminationPolicy( ) {
    return excessCapacityTerminationPolicy;
  }

  public void setExcessCapacityTerminationPolicy( final String excessCapacityTerminationPolicy ) {
    this.excessCapacityTerminationPolicy = excessCapacityTerminationPolicy;
  }

  public String getFleetId( ) {
    return fleetId;
  }

  public void setFleetId( final String fleetId ) {
    this.fleetId = fleetId;
  }

  public TargetCapacitySpecificationRequest getTargetCapacitySpecification( ) {
    return targetCapacitySpecification;
  }

  public void setTargetCapacitySpecification( final TargetCapacitySpecificationRequest targetCapacitySpecification ) {
    this.targetCapacitySpecification = targetCapacitySpecification;
  }

}
