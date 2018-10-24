/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class LoadPermissionList extends EucalyptusData {

  private ArrayList<LoadPermission> member = new ArrayList<LoadPermission>();

  public ArrayList<LoadPermission> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<LoadPermission> member ) {
    this.member = member;
  }
}
