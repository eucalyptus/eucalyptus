/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class ProductCodeList extends EucalyptusData {

  private ArrayList<ProductCode> member = new ArrayList<ProductCode>();

  public ArrayList<ProductCode> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<ProductCode> member ) {
    this.member = member;
  }
}
