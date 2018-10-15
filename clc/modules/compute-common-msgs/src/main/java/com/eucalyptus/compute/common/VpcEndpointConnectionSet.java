/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class VpcEndpointConnectionSet extends EucalyptusData {

  private ArrayList<VpcEndpointConnection> member = new ArrayList<VpcEndpointConnection>();

  public ArrayList<VpcEndpointConnection> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<VpcEndpointConnection> member ) {
    this.member = member;
  }
}
