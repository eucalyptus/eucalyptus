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


public class LaunchTemplateInstanceNetworkInterfaceSpecificationRequestList extends EucalyptusData {

  @HttpParameterMapping( parameter = "NetworkInterface" )
  @HttpEmbedded( multiple = true )
  private ArrayList<LaunchTemplateInstanceNetworkInterfaceSpecificationRequest> member = new ArrayList<LaunchTemplateInstanceNetworkInterfaceSpecificationRequest>();

  public ArrayList<LaunchTemplateInstanceNetworkInterfaceSpecificationRequest> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<LaunchTemplateInstanceNetworkInterfaceSpecificationRequest> member ) {
    this.member = member;
  }
}
