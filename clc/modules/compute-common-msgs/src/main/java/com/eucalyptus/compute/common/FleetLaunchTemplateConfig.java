/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class FleetLaunchTemplateConfig extends EucalyptusData {

  private FleetLaunchTemplateSpecification launchTemplateSpecification;
  private FleetLaunchTemplateOverridesList overrides;

  public FleetLaunchTemplateSpecification getLaunchTemplateSpecification( ) {
    return launchTemplateSpecification;
  }

  public void setLaunchTemplateSpecification( final FleetLaunchTemplateSpecification launchTemplateSpecification ) {
    this.launchTemplateSpecification = launchTemplateSpecification;
  }

  public FleetLaunchTemplateOverridesList getOverrides( ) {
    return overrides;
  }

  public void setOverrides( final FleetLaunchTemplateOverridesList overrides ) {
    this.overrides = overrides;
  }

}
