/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class ServiceConfigurationSet extends EucalyptusData {

  private ArrayList<ServiceConfiguration> member = new ArrayList<ServiceConfiguration>();

  public ArrayList<ServiceConfiguration> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<ServiceConfiguration> member ) {
    this.member = member;
  }
}
