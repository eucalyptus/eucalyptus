/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class LaunchTemplateVersionSet extends EucalyptusData {

  private ArrayList<LaunchTemplateVersion> member = new ArrayList<LaunchTemplateVersion>();

  public ArrayList<LaunchTemplateVersion> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<LaunchTemplateVersion> member ) {
    this.member = member;
  }
}
