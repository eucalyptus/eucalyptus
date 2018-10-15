/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class FleetLaunchTemplateConfigListRequest extends EucalyptusData {

  private ArrayList<FleetLaunchTemplateConfigRequest> member = new ArrayList<FleetLaunchTemplateConfigRequest>();

  public ArrayList<FleetLaunchTemplateConfigRequest> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<FleetLaunchTemplateConfigRequest> member ) {
    this.member = member;
  }
}
