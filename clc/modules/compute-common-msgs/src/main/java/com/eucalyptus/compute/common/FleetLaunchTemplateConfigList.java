/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class FleetLaunchTemplateConfigList extends EucalyptusData {

  private ArrayList<FleetLaunchTemplateConfig> member = new ArrayList<FleetLaunchTemplateConfig>();

  public ArrayList<FleetLaunchTemplateConfig> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<FleetLaunchTemplateConfig> member ) {
    this.member = member;
  }
}
