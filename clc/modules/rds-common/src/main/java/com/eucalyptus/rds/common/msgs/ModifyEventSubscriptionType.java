/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class ModifyEventSubscriptionType extends RdsMessage {

  private Boolean enabled;

  private EventCategoriesList eventCategories;

  private String snsTopicArn;

  private String sourceType;

  @Nonnull
  private String subscriptionName;

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

}
