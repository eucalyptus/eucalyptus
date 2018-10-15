/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class FleetLaunchTemplateOverridesListRequest extends EucalyptusData {

  private ArrayList<FleetLaunchTemplateOverridesRequest> member = new ArrayList<FleetLaunchTemplateOverridesRequest>();

  public ArrayList<FleetLaunchTemplateOverridesRequest> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<FleetLaunchTemplateOverridesRequest> member ) {
    this.member = member;
  }
}
