/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class EventCategoriesMap extends EucalyptusData {

  private EventCategoriesList eventCategories;

  private String sourceType;

  public EventCategoriesList getEventCategories() {
    return eventCategories;
  }

  public void setEventCategories(final EventCategoriesList eventCategories) {
    this.eventCategories = eventCategories;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(final String sourceType) {
    this.sourceType = sourceType;
  }

}
