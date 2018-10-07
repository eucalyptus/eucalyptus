/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import com.eucalyptus.compute.common.EventInformation;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class HistoryRecordEntry extends EucalyptusData {

  private EventInformation eventInformation;
  private String eventType;
  private java.util.Date timestamp;

  public EventInformation getEventInformation( ) {
    return eventInformation;
  }

  public void setEventInformation( final EventInformation eventInformation ) {
    this.eventInformation = eventInformation;
  }

  public String getEventType( ) {
    return eventType;
  }

  public void setEventType( final String eventType ) {
    this.eventType = eventType;
  }

  public java.util.Date getTimestamp( ) {
    return timestamp;
  }

  public void setTimestamp( final java.util.Date timestamp ) {
    this.timestamp = timestamp;
  }

}
