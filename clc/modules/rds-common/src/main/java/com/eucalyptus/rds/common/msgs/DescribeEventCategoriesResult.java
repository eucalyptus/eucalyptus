/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeEventCategoriesResult extends EucalyptusData {

  private EventCategoriesMapList eventCategoriesMapList;

  public EventCategoriesMapList getEventCategoriesMapList() {
    return eventCategoriesMapList;
  }

  public void setEventCategoriesMapList(final EventCategoriesMapList eventCategoriesMapList) {
    this.eventCategoriesMapList = eventCategoriesMapList;
  }

}
