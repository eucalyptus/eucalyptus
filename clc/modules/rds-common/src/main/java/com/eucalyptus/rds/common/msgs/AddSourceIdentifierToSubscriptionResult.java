/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class AddSourceIdentifierToSubscriptionResult extends EucalyptusData {

  private EventSubscription eventSubscription;

  public EventSubscription getEventSubscription() {
    return eventSubscription;
  }

  public void setEventSubscription(final EventSubscription eventSubscription) {
    this.eventSubscription = eventSubscription;
  }

}
