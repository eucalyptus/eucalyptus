/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.reporting.modules.storage;

import static com.eucalyptus.reporting.event.SnapShotEvent.CreateActionInfo;
import javax.annotation.Nonnull;

import com.eucalyptus.reporting.service.ReportingService;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.SnapShotEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotEventStore;
import com.eucalyptus.reporting.domain.ReportingUserCrud;
import com.eucalyptus.reporting.domain.ReportingAccountCrud;

import com.google.common.base.Preconditions;

public class SnapShotUsageEventListener implements EventListener<SnapShotEvent> {

  private static Logger LOG = Logger.getLogger( SnapShotUsageEventListener.class );

  public static void register() {
    Listeners.register( SnapShotEvent.class, new SnapShotUsageEventListener() );
  }

  @Override
  public void fireEvent( @Nonnull final SnapShotEvent event ) {
    if (!ReportingService.DATA_COLLECTION_ENABLED) {
      ReportingService.faultDisableReportingServiceIfNecessary();
      LOG.trace("Reporting service data collection disabled....SnapShotEvent discarded");
      return;
    }
    Preconditions.checkNotNull(event, "Event is required");

    final long timeInMs = getCurrentTimeMillis();

    try {
      getReportingAccountCrud().createOrUpdateAccount( event.getAccountNumber( ),
           lookupAccountAliasById( event.getAccountNumber( ) ) );
      getReportingUserCrud( ).createOrUpdateUser( event.getUserId( ), event
          .getAccountNumber( ), event.getUserName( ) );

      final ReportingVolumeSnapshotEventStore eventStore = getReportingVolumeSnapshotEventStore();
      switch (event.getActionInfo().getAction()) {
        case SNAPSHOTCREATE:
          CreateActionInfo eventActionInfo = (CreateActionInfo)event.getActionInfo();
          eventStore.insertCreateEvent(
              event.getUuid(),
              eventActionInfo.getVolumeUuid(),
              event.getSnapshotId(),
              timeInMs,
              event.getUserId(),
              eventActionInfo.getSize() );
          break;
        case SNAPSHOTDELETE:
          eventStore.insertDeleteEvent(
              event.getUuid(),
              timeInMs);
          break;
      }
    } catch (AuthException e) {
        LOG.error("Unable fire snap shot reporting event", e.getCause());
    }
  }

  protected ReportingAccountCrud getReportingAccountCrud() {
    return ReportingAccountCrud.getInstance();
  }

  protected ReportingUserCrud getReportingUserCrud() {
    return ReportingUserCrud.getInstance();
  }

  protected ReportingVolumeSnapshotEventStore getReportingVolumeSnapshotEventStore() {
    return ReportingVolumeSnapshotEventStore.getInstance();
  }

  protected long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }

  protected String lookupAccountAliasById( final String accountNumber ) throws AuthException {
    return Accounts.lookupAccountAliasById( accountNumber );
  }
}
