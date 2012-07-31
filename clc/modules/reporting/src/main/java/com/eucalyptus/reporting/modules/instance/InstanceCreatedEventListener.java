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
import com.eucalyptus.reporting.event.InstanceCreatedEvent;
import com.eucalyptus.reporting.event_store.ReportingInstanceEventStore;
import com.eucalyptus.reporting.user.ReportingAccountDao;
import com.eucalyptus.reporting.user.ReportingUserDao;
import com.google.common.base.Preconditions;

/**
 * //TODO:STEVE: Remove this and the associated InstanceCreatedEvent?
 */
public class InstanceCreatedEventListener implements EventListener<InstanceCreatedEvent> {

  public static void register( ) {
    Listeners.register( InstanceCreatedEvent.class, new InstanceCreatedEventListener() );
  }

  @Override
  public void fireEvent( @Nonnull final InstanceCreatedEvent event ) {
    Preconditions.checkNotNull( event, "Event is required" );

    // Ensure account / user info is present and up to date
    ReportingAccountDao.getInstance().addUpdateAccount( event.getAccountId(), event.getAccountName() );
    ReportingUserDao.getInstance().addUpdateUser( event.getUserId(), event.getUserName() );

    final ReportingInstanceEventStore eventStore = ReportingInstanceEventStore.getInstance();
    eventStore.insertCreateEvent(
        event.getUuid(),
        System.currentTimeMillis(),
        event.getInstanceId(),
        event.getInstanceType(),
        event.getUserId(),
        event.getClusterName(),
        event.getAvailabilityZone()
        );
  }
}
