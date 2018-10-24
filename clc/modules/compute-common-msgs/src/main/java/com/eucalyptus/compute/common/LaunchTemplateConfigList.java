/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class LaunchTemplateConfigList extends EucalyptusData {

  private ArrayList<LaunchTemplateConfig> member = new ArrayList<LaunchTemplateConfig>();

  public ArrayList<LaunchTemplateConfig> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<LaunchTemplateConfig> member ) {
    this.member = member;
  }
}
