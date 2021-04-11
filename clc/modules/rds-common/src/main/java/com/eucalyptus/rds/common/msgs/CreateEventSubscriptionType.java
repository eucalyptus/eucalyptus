/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CreateEventSubscriptionType extends RdsMessage {

  private Boolean enabled;

  private EventCategoriesList eventCategories;

  @Nonnull
  private String snsTopicArn;

  private SourceIdsList sourceIds;

  private String sourceType;

  @Nonnull
  private String subscriptionName;

  private TagList tags;

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(final Boolean enabled) {
    this.enabled = enabled;
  }

  public EventCategoriesList getEventCategories() {
    return eventCategories;
  }

  public void setEventCategories(final EventCategoriesList eventCategories) {
    this.eventCategories = eventCategories;
  }

  public String getSnsTopicArn() {
    return snsTopicArn;
  }

  public void setSnsTopicArn(final String snsTopicArn) {
    this.snsTopicArn = snsTopicArn;
  }

  public SourceIdsList getSourceIds() {
    return sourceIds;
  }

  public void setSourceIds(final SourceIdsList sourceIds) {
    this.sourceIds = sourceIds;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(final String sourceType) {
    this.sourceType = sourceType;
  }

  public String getSubscriptionName() {
    return subscriptionName;
  }

  public void setSubscriptionName(final String subscriptionName) {
    this.subscriptionName = subscriptionName;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

}
