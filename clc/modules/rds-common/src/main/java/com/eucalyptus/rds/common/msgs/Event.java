/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Event extends EucalyptusData {

  private java.util.Date date;

  private EventCategoriesList eventCategories;

  private String message;

  private String sourceArn;

  private String sourceIdentifier;

  @FieldRegex(FieldRegexValue.ENUM_SOURCETYPE)
  private String sourceType;

  public java.util.Date getDate() {
    return date;
  }

  public void setDate(final java.util.Date date) {
    this.date = date;
  }

  public EventCategoriesList getEventCategories() {
    return eventCategories;
  }

  public void setEventCategories(final EventCategoriesList eventCategories) {
    this.eventCategories = eventCategories;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  public String getSourceArn() {
    return sourceArn;
  }

  public void setSourceArn(final String sourceArn) {
    this.sourceArn = sourceArn;
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

}
