/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class LaunchTemplateTagSpecificationRequestList extends EucalyptusData {

  private ArrayList<LaunchTemplateTagSpecificationRequest> member = new ArrayList<LaunchTemplateTagSpecificationRequest>();

  public ArrayList<LaunchTemplateTagSpecificationRequest> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<LaunchTemplateTagSpecificationRequest> member ) {
    this.member = member;
  }
}
