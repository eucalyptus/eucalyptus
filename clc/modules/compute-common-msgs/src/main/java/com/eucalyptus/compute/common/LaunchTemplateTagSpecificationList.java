/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class LaunchTemplateTagSpecificationList extends EucalyptusData {

  private ArrayList<LaunchTemplateTagSpecification> member = new ArrayList<LaunchTemplateTagSpecification>();

  public ArrayList<LaunchTemplateTagSpecification> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<LaunchTemplateTagSpecification> member ) {
    this.member = member;
  }
}
