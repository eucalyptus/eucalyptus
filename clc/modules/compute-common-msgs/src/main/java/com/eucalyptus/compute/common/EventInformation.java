/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
