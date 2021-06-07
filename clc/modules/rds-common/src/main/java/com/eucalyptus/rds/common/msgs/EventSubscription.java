/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class EventSubscription extends EucalyptusData {

  private String custSubscriptionId;

  private String customerAwsId;

  private Boolean enabled;

  private EventCategoriesList eventCategoriesList;

  private String eventSubscriptionArn;

  private String snsTopicArn;

  private SourceIdsList sourceIdsList;

  private String sourceType;

  private String status;

  private String subscriptionCreationTime;

  public String getCustSubscriptionId() {
    return custSubscriptionId;
  }

  public void setCustSubscriptionId(final String custSubscriptionId) {
    this.custSubscriptionId = custSubscriptionId;
  }

  public String getCustomerAwsId() {
    return customerAwsId;
  }

  public void setCustomerAwsId(final String customerAwsId) {
    this.customerAwsId = customerAwsId;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(final Boolean enabled) {
    this.enabled = enabled;
  }

  public EventCategoriesList getEventCategoriesList() {
    return eventCategoriesList;
  }

  public void setEventCategoriesList(final EventCategoriesList eventCategoriesList) {
    this.eventCategoriesList = eventCategoriesList;
  }

  public String getEventSubscriptionArn() {
    return eventSubscriptionArn;
  }

  public void setEventSubscriptionArn(final String eventSubscriptionArn) {
    this.eventSubscriptionArn = eventSubscriptionArn;
  }

  public String getSnsTopicArn() {
    return snsTopicArn;
  }

  public void setSnsTopicArn(final String snsTopicArn) {
    this.snsTopicArn = snsTopicArn;
  }

  public SourceIdsList getSourceIdsList() {
    return sourceIdsList;
  }

  public void setSourceIdsList(final SourceIdsList sourceIdsList) {
    this.sourceIdsList = sourceIdsList;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(final String sourceType) {
    this.sourceType = sourceType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public String getSubscriptionCreationTime() {
    return subscriptionCreationTime;
  }

  public void setSubscriptionCreationTime(final String subscriptionCreationTime) {
    this.subscriptionCreationTime = subscriptionCreationTime;
  }

}
