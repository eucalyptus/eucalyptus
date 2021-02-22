/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;


public class DescribeEventsType extends RdsMessage {

  private Integer duration;

  private java.util.Date endTime;

  private EventCategoriesList eventCategories;

  private FilterList filters;

  private String marker;

  private Integer maxRecords;

  private String sourceIdentifier;

  @FieldRegex(FieldRegexValue.ENUM_SOURCETYPE)
  private String sourceType;

  private java.util.Date startTime;

  public Integer getDuration() {
    return duration;
  }

  public void setDuration(final Integer duration) {
    this.duration = duration;
  }

  public java.util.Date getEndTime() {
    return endTime;
  }

  public void setEndTime(final java.util.Date endTime) {
    this.endTime = endTime;
  }

  public EventCategoriesList getEventCategories() {
    return eventCategories;
  }

  public void setEventCategories(final EventCategoriesList eventCategories) {
    this.eventCategories = eventCategories;
  }

  public FilterList getFilters() {
    return filters;
  }

  public void setFilters(final FilterList filters) {
    this.filters = filters;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public Integer getMaxRecords() {
    return maxRecords;
  }

  public void setMaxRecords(final Integer maxRecords) {
    this.maxRecords = maxRecords;
  }

  public String getSourceIdentifier() {
    return sourceIdentifier;
  }

  public void setSourceIdentifier(final String sourceIdentifier) {
    this.sourceIdentifier = sourceIdentifier;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(final String sourceType) {
    this.sourceType = sourceType;
  }

  public java.util.Date getStartTime() {
    return startTime;
  }

  public void setStartTime(final java.util.Date startTime) {
    this.startTime = startTime;
  }

}
