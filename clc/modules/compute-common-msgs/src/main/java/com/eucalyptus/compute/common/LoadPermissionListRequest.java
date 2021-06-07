/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class LoadPermissionListRequest extends EucalyptusData {

  private ArrayList<LoadPermissionRequest> member = new ArrayList<LoadPermissionRequest>();

  public ArrayList<LoadPermissionRequest> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<LoadPermissionRequest> member ) {
    this.member = member;
  }
}
