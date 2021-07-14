/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;

public class VpcClassicLinkSetType extends EucalyptusData {

  private ArrayList<VpcClassicLinkType> item = new ArrayList<VpcClassicLinkType>( );

  public ArrayList<VpcClassicLinkType> getItem( ) {
    return item;
  }

  public void setItem( ArrayList<VpcClassicLinkType> item ) {
    this.item = item;
  }
}
