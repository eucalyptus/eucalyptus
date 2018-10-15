/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class FleetLaunchTemplateConfigRequest extends EucalyptusData {

  private FleetLaunchTemplateSpecificationRequest launchTemplateSpecification;
  private FleetLaunchTemplateOverridesListRequest overrides;

  public FleetLaunchTemplateSpecificationRequest getLaunchTemplateSpecification( ) {
    return launchTemplateSpecification;
  }

  public void setLaunchTemplateSpecification( final FleetLaunchTemplateSpecificationRequest launchTemplateSpecification ) {
    this.launchTemplateSpecification = launchTemplateSpecification;
  }

  public FleetLaunchTemplateOverridesListRequest getOverrides( ) {
    return overrides;
  }

  public void setOverrides( final FleetLaunchTemplateOverridesListRequest overrides ) {
    this.overrides = overrides;
  }

}
