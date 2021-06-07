/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class AllowedPrincipalSet extends EucalyptusData {

  private ArrayList<AllowedPrincipal> member = new ArrayList<AllowedPrincipal>();

  public ArrayList<AllowedPrincipal> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<AllowedPrincipal> member ) {
    this.member = member;
  }
}
