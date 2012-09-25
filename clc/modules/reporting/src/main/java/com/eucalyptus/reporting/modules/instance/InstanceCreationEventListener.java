/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.reporting.modules.instance;

import javax.annotation.Nonnull;

import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.domain.ReportingAccountCrud;
import com.eucalyptus.reporting.domain.ReportingUserCrud;
import com.eucalyptus.reporting.event.InstanceCreationEvent;
import com.eucalyptus.reporting.event_store.ReportingInstanceEventStore;
import com.google.common.base.Preconditions;

public class InstanceCreationEventListener implements
    EventListener<InstanceCreationEvent> {

  public static void register() {
    Listeners.register( InstanceCreationEvent.class,
        new InstanceCreationEventListener() );
  }

  @Override
  public void fireEvent( @Nonnull final InstanceCreationEvent event ) {
    Preconditions.checkNotNull( event, "Event is required" );

    final long timestamp = getCurrentTimeMillis();

    // Ensure account / user info is present and up to date
    getReportingAccountCrud().createOrUpdateAccount( event.getAccountId(),
        event.getAccountName() );
    getReportingUserCrud().createOrUpdateUser( event.getUserId(),
        event.getAccountId(), event.getUserName() );

    // Record creation
    ReportingInstanceEventStore eventStore = getReportingInstanceEventStore();
    eventStore.insertCreateEvent( event.getUuid(), event.getInstanceId(), timestamp,
        event.getInstanceType(), event.getUserId(), event.getAvailabilityZone() );

  }

  protected ReportingAccountCrud getReportingAccountCrud() {
    return ReportingAccountCrud.getInstance();
  }

  protected ReportingUserCrud getReportingUserCrud() {
    return ReportingUserCrud.getInstance();
  }

  protected ReportingInstanceEventStore getReportingInstanceEventStore() {
    return ReportingInstanceEventStore.getInstance();
  }

  /**
   * Get the current time which will be used for recording when an event
   * occurred. This can be overridden if you have some alternative method of
   * timekeeping (synchronized, test times, etc).
   */
  protected long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }
}
