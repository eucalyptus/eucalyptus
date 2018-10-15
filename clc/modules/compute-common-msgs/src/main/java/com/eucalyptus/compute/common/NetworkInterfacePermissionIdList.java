/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;
import com.eucalyptus.binding.HttpParameterMapping;


public class NetworkInterfacePermissionIdList extends EucalyptusData {

  @HttpParameterMapping( parameter = "NetworkInterfacePermissionId" )
  private ArrayList<String> member = new ArrayList<String>();

  public ArrayList<String> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<String> member ) {
    this.member = member;
  }
}
