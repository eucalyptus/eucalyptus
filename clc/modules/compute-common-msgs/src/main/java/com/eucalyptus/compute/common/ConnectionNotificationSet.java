/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class ConnectionNotificationSet extends EucalyptusData {

  private ArrayList<ConnectionNotification> member = new ArrayList<ConnectionNotification>();

  public ArrayList<ConnectionNotification> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<ConnectionNotification> member ) {
    this.member = member;
  }
}
