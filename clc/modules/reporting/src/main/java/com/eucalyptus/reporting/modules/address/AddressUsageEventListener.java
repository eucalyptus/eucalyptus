/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
package com.eucalyptus.reporting.modules.address;

import static com.eucalyptus.reporting.event.EventActionInfo.InstanceEventActionInfo;
import javax.annotation.Nonnull;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.AddressEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpEventStore;
import com.eucalyptus.reporting.domain.ReportingAccountCrud;
import com.eucalyptus.reporting.domain.ReportingUserCrud;
import com.eucalyptus.reporting.service.ReportingService;
import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;

/**
 * Address event listener for user actions.
 */
public class AddressUsageEventListener implements EventListener<AddressEvent> {
  private static final Logger LOG = Logger.getLogger(AddressUsageEventListener.class);
  public static void register( ) {
    Listeners.register( AddressEvent.class, new AddressUsageEventListener() );
  }

  @Override
  public void fireEvent( @Nonnull final AddressEvent event ) {
    if (!ReportingService.DATA_COLLECTION_ENABLED) {
      ReportingService.faultDisableReportingServiceIfNecessary();
      LOG.trace("Reporting service data collection disabled....AddressUsageEvent discarded");
      return;
    }
    Preconditions.checkNotNull( event, "Event is required" );

    final long timestamp = getCurrentTimeMillis();

    // Ensure account / user info is present and up to date
    getReportingAccountCrud().createOrUpdateAccount( event.getAccountId(), event.getAccountName() );
    getReportingUserCrud().createOrUpdateUser( event.getUserId(), event.getAccountId(), event.getUserName() );

    final ReportingElasticIpEventStore eventStore = getReportingElasticIpEventStore();
    switch (event.getActionInfo().getAction()) {
      case ALLOCATE:
        eventStore.insertCreateEvent( timestamp, event.getUserId(), event.getAddress() );
        break;
      case RELEASE:
        eventStore.insertDeleteEvent( event.getAddress(), timestamp );
        break;
      case ASSOCIATE:
        eventStore.insertAttachEvent( event.getAddress(), ((InstanceEventActionInfo)event.getActionInfo()).getInstanceUuid(), timestamp );
        break;
      case DISASSOCIATE:
        eventStore.insertDetachEvent( event.getAddress(), ((InstanceEventActionInfo)event.getActionInfo()).getInstanceUuid(), timestamp );
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
