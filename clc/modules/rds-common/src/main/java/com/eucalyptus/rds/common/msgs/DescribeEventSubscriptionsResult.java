/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeEventSubscriptionsResult extends EucalyptusData {

  private EventSubscriptionsList eventSubscriptionsList;

  private String marker;

  public EventSubscriptionsList getEventSubscriptionsList() {
    return eventSubscriptionsList;
  }

  public void setEventSubscriptionsList(final EventSubscriptionsList eventSubscriptionsList) {
    this.eventSubscriptionsList = eventSubscriptionsList;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

}
