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

package com.eucalyptus.reporting.modules.storage;

import static com.eucalyptus.reporting.event.EventActionInfo.InstanceEventActionInfo;
import static com.eucalyptus.reporting.event.VolumeEvent.VolumeAction;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.EventActionInfo;
import com.eucalyptus.reporting.event.VolumeEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeEventStore;
import com.eucalyptus.reporting.domain.ReportingAccountCrud;
import com.eucalyptus.reporting.domain.ReportingUserCrud;

import com.google.common.base.Preconditions;

/**
 * Volume event listener for user actions.
 */
public class VolumeUsageEventListener implements EventListener<VolumeEvent> {

  private static Logger LOG = Logger.getLogger(VolumeUsageEventListener.class);

  public static void register() {
    Listeners.register( VolumeEvent.class, new VolumeUsageEventListener() );
  }

  @Override
  public void fireEvent(@Nonnull final VolumeEvent event) {
    Preconditions.checkNotNull(event, "Event is required");

    final long timeInMs = getCurrentTimeMillis();
    try {
      final User user = lookupUser(event.getOwner().getUserId());

      getReportingAccountCrud().createOrUpdateAccount(user.getAccount()
          .getName(), user.getAccount().getAccountNumber());
      getReportingUserCrud().createOrUpdateUser(user.getUserId(), user
          .getAccount().getAccountNumber(), user.getName());

      final ReportingVolumeEventStore eventStore = getReportingVolumeEventStore();
      switch (event.getActionInfo().getAction()) {
        case VOLUMECREATE:
          eventStore.insertCreateEvent(event.getUuid(), event
              .getVolumeId(), timeInMs, event.getOwner()
              .getUserId(), event.getAvailabilityZone(), event
              .getSizeGB());
          break;
        case VOLUMEDELETE:
          eventStore.insertDeleteEvent(event.getUuid(), timeInMs);
          break;
        case VOLUMEATTACH:
          eventStore
              .insertAttachEvent(event.getUuid(),
                  ((EventActionInfo.InstanceEventActionInfo<VolumeAction>) event.getActionInfo())
                      .getInstanceUuid(), event.getSizeGB(),
                  timeInMs);
          break;
        case VOLUMEDETACH:
          eventStore
              .insertDetachEvent(event.getUuid(),
                  ((InstanceEventActionInfo) event.getActionInfo())
                      .getInstanceUuid(),
                  timeInMs);
          break;
      }
    } catch (AuthException e) {
        LOG.error("Unable to fire volume reporting event.", e.getCause());
    }
  }

  protected ReportingAccountCrud getReportingAccountCrud() {
    return ReportingAccountCrud.getInstance();
  }

  protected ReportingUserCrud getReportingUserCrud() {
    return ReportingUserCrud.getInstance();
  }

  protected ReportingVolumeEventStore getReportingVolumeEventStore() {
    return ReportingVolumeEventStore.getInstance();
  }

  protected long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }

  protected User lookupUser( final String userId ) throws AuthException {
    return Accounts.lookupUserById( userId );
  }
}
