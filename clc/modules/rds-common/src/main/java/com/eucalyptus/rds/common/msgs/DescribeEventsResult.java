/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeEventsResult extends EucalyptusData {

  private EventList events;

  private String marker;

  public EventList getEvents() {
    return events;
  }

  public void setEvents(final EventList events) {
    this.events = events;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
