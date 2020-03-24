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


public class LaunchTemplateTagSpecificationRequestList extends EucalyptusData {

  @HttpParameterMapping( parameter = "TagSpecification" )
  @HttpEmbedded( multiple = true )
  private ArrayList<LaunchTemplateTagSpecificationRequest> member = new ArrayList<LaunchTemplateTagSpecificationRequest>();

  public ArrayList<LaunchTemplateTagSpecificationRequest> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<LaunchTemplateTagSpecificationRequest> member ) {
    this.member = member;
  }
}
