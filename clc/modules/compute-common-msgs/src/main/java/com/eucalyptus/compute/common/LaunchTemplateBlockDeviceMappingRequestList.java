/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;


public class LaunchTemplateBlockDeviceMappingRequestList extends EucalyptusData {

  @HttpParameterMapping( parameter = "BlockDeviceMapping" )
  @HttpEmbedded( multiple = true )
  private ArrayList<LaunchTemplateBlockDeviceMappingRequest> member = new ArrayList<LaunchTemplateBlockDeviceMappingRequest>();

  public ArrayList<LaunchTemplateBlockDeviceMappingRequest> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<LaunchTemplateBlockDeviceMappingRequest> member ) {
    this.member = member;
  }
}
