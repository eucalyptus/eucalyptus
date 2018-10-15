/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class LaunchTemplateOverridesList extends EucalyptusData {

  private ArrayList<LaunchTemplateOverrides> member = new ArrayList<LaunchTemplateOverrides>();

  public ArrayList<LaunchTemplateOverrides> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<LaunchTemplateOverrides> member ) {
    this.member = member;
  }
}
