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
import com.eucalyptus.objectstorage.util.WalrusProperties;
import com.eucalyptus.reporting.domain.ReportingAccountCrud;
import com.eucalyptus.reporting.domain.ReportingUserCrud;
import com.eucalyptus.reporting.event.S3ObjectEvent;
import com.eucalyptus.reporting.event_store.ReportingS3ObjectEventStore;
import com.google.common.base.Preconditions;

public class S3ObjectUsageEventListener implements EventListener<S3ObjectEvent>{

    private static Logger LOG = Logger.getLogger( S3ObjectUsageEventListener.class );

    public static void register() {
      Listeners.register( S3ObjectEvent.class, new S3ObjectUsageEventListener() );
    }

    @Override
    public void fireEvent( @Nonnull final S3ObjectEvent event ) {
      Preconditions.checkNotNull(event, "Event is required");

      final long timeInMs = getCurrentTimeMillis();

      try {
        final User user = lookupUser( event.getOwnerUserId() );

        getReportingAccountCrud().createOrUpdateAccount(user.getAccount().getAccountNumber(),
            user.getAccount().getName());
        getReportingUserCrud().createOrUpdateUser(user.getUserId(), user
            .getAccount().getAccountNumber(), user.getName());

        final ReportingS3ObjectEventStore eventStore = getReportingS3ObjectEventStore();
        switch (event.getAction()) {
          case OBJECTCREATE:
            eventStore.insertS3ObjectCreateEvent(
                event.getBucketName(),
                event.getObjectKey(),
                toReportingVersion( event.getVersion() ),
                event.getSize(),
                timeInMs,
                event.getOwnerUserId());
            break;
          case OBJECTDELETE:
            eventStore.insertS3ObjectDeleteEvent(
                event.getBucketName(),
                event.getObjectKey(),
                toReportingVersion( event.getVersion() ),
                timeInMs);
            break;
        }
      } catch (AuthException e) {
          LOG.error("Unable fire s3 object reporting event", e.getCause());
      }
    }

    protected ReportingAccountCrud getReportingAccountCrud() {
      return ReportingAccountCrud.getInstance();
    }

    protected ReportingUserCrud getReportingUserCrud() {
      return ReportingUserCrud.getInstance();
    }

    protected ReportingS3ObjectEventStore getReportingS3ObjectEventStore() {
      return ReportingS3ObjectEventStore.getInstance();
    }

    protected long getCurrentTimeMillis() {
      return System.currentTimeMillis();
    }

    protected User lookupUser( final String userId ) throws AuthException {
      return Accounts.lookupUserById( userId );
    }

    private String toReportingVersion( final String version ) {
      if ( WalrusProperties.NULL_VERSION_ID.equals( version ) ) {
        return null;
      }
      return version;
    }
}
