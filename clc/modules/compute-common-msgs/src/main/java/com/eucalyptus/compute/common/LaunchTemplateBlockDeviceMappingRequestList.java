/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class LaunchTemplateBlockDeviceMappingRequestList extends EucalyptusData {

  private ArrayList<LaunchTemplateBlockDeviceMappingRequest> member = new ArrayList<LaunchTemplateBlockDeviceMappingRequest>();

  public ArrayList<LaunchTemplateBlockDeviceMappingRequest> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<LaunchTemplateBlockDeviceMappingRequest> member ) {
    this.member = member;
  }
}
