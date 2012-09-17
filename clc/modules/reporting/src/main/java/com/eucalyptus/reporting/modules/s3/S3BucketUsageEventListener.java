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

package com.eucalyptus.reporting.modules.s3;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.domain.ReportingAccountCrud;
import com.eucalyptus.reporting.domain.ReportingUserCrud;
import com.eucalyptus.reporting.event.S3BucketEvent;
import com.eucalyptus.reporting.event_store.ReportingS3BucketEventStore;
import com.google.common.base.Preconditions;

public class S3BucketUsageEventListener implements EventListener<S3BucketEvent>{

    private static Logger LOG = Logger.getLogger( S3BucketUsageEventListener.class );

    public static void register() {
      Listeners.register( S3BucketEvent.class, new S3BucketUsageEventListener() );
    }

    @Override
    public void fireEvent( @Nonnull final S3BucketEvent event ) {
      Preconditions.checkNotNull(event, "Event is required");

      final long timeInMs = getCurrentTimeMillis();

      try {
        final User user = lookupUser( event.getOwner().getUserId() );

        getReportingAccountCrud().createOrUpdateAccount(user.getAccount()
            .getName(), user.getAccount().getAccountNumber());
        getReportingUserCrud().createOrUpdateUser(user.getUserId(), user
            .getAccount().getAccountNumber(), user.getName());

        final ReportingS3BucketEventStore eventStore = getReportingS3BucketEventStore();
        switch (event.getAction()) {
          case BUCKETCREATE:
            eventStore.insertS3BucketCreateEvent( event.getBucketName(), event.getOwner().getUserId(), timeInMs);
            break;
          case BUCKETDELETE:
            eventStore.insertS3BucketDeleteEvent(event.getBucketName(), timeInMs); 
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

    protected ReportingS3BucketEventStore getReportingS3BucketEventStore() {
      return ReportingS3BucketEventStore.getInstance();
    }

    protected long getCurrentTimeMillis() {
      return System.currentTimeMillis();
    }

    protected User lookupUser( final String userId ) throws AuthException {
      return Accounts.lookupUserById( userId );
    }
    
}
