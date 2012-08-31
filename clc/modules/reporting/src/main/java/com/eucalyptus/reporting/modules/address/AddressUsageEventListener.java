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
package com.eucalyptus.reporting.modules.address;

import static com.eucalyptus.reporting.event.EventActionInfo.InstanceEventActionInfo;
import javax.annotation.Nonnull;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.AddressEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpEventStore;
import com.eucalyptus.reporting.domain.ReportingAccountCrud;
import com.eucalyptus.reporting.domain.ReportingUserCrud;
import com.google.common.base.Preconditions;

/**
 * Address event listener for user actions.
 */
public class AddressUsageEventListener implements EventListener<AddressEvent> {

  public static void register( ) {
    Listeners.register( AddressEvent.class, new AddressUsageEventListener() );
  }

  @Override
  public void fireEvent( @Nonnull final AddressEvent event ) {
    Preconditions.checkNotNull( event, "Event is required" );

    final long timestamp = getCurrentTimeMillis();

    // Ensure account / user info is present and up to date
    getReportingAccountCrud().createOrUpdateAccount( event.getAccountId(), event.getAccountName() );
    getReportingUserCrud().createOrUpdateUser( event.getUserId(), event.getAccountId(), event.getUserName() );

    final ReportingElasticIpEventStore eventStore = getReportingElasticIpEventStore();
    switch (event.getActionInfo().getAction()) {
      case ALLOCATE:
        eventStore.insertCreateEvent( event.getUuid(), timestamp, event.getUserId(), event.getAddress() );
        break;
      case RELEASE:
        eventStore.insertDeleteEvent( event.getUuid(), timestamp );
        break;
      case ASSOCIATE:
        eventStore.insertAttachEvent( event.getUuid(), ((InstanceEventActionInfo)event.getActionInfo()).getInstanceUuid(), timestamp );
        break;
      case DISASSOCIATE:
        eventStore.insertDetachEvent( event.getUuid(), ((InstanceEventActionInfo)event.getActionInfo()).getInstanceUuid(), timestamp );
        break;
    }
  }

  protected ReportingAccountCrud getReportingAccountCrud() {
    return ReportingAccountCrud.getInstance();
  }

  protected ReportingUserCrud getReportingUserCrud() {
    return ReportingUserCrud.getInstance();
  }

  protected ReportingElasticIpEventStore getReportingElasticIpEventStore() {
    return ReportingElasticIpEventStore.getInstance();
  }

  /**
   * Get the current time which will be used for recording when an event
   * occurred. This can be overridden if you have some alternative method
   * of timekeeping (synchronized, test times, etc).
   */
  protected long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }
}
