/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class EventInformation extends EucalyptusData {

  private String eventDescription;
  private String eventSubType;
  private String instanceId;

  public String getEventDescription( ) {
    return eventDescription;
  }

  public void setEventDescription( final String eventDescription ) {
    this.eventDescription = eventDescription;
  }

  public String getEventSubType( ) {
    return eventSubType;
  }

  public void setEventSubType( final String eventSubType ) {
    this.eventSubType = eventSubType;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( final String instanceId ) {
    this.instanceId = instanceId;
  }

}
