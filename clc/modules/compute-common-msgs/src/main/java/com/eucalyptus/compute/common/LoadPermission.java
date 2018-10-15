/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LoadPermission extends EucalyptusData {

  private String group;
  private String userId;

  public String getGroup( ) {
    return group;
  }

  public void setGroup( final String group ) {
    this.group = group;
  }

  public String getUserId( ) {
    return userId;
  }

  public void setUserId( final String userId ) {
    this.userId = userId;
  }

}
