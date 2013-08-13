/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.reporting.modules.s3

import org.junit.Test
import com.eucalyptus.auth.principal.Principals

import static org.junit.Assert.*
import com.eucalyptus.reporting.domain.ReportingAccountCrud
import com.eucalyptus.reporting.domain.ReportingUserCrud
import com.eucalyptus.auth.principal.User
import com.eucalyptus.reporting.event.S3ObjectEvent
import com.eucalyptus.reporting.event_store.ReportingS3ObjectEventStore
import com.eucalyptus.reporting.event_store.ReportingS3ObjectCreateEvent
import com.eucalyptus.reporting.event_store.ReportingS3ObjectDeleteEvent
import com.eucalyptus.objectstorage.util.WalrusProperties

/**
 * 
 */
class S3ObjectUsageEventListenerTest {

  @Test
  void testInstantiable() {
    new S3ObjectUsageEventListener()
  }

  @Test
  void testCreateEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( S3ObjectEvent.with(
        S3ObjectEvent.forS3ObjectCreate(),
        "bucket15",
        "object34",
        "version1",
        Principals.systemFullName().getUserId(),
        Integer.MAX_VALUE.toLong() + 1L
    ), timestamp )

    assertTrue( "Persisted event is ReportingS3BucketCreateEvent", persisted instanceof ReportingS3ObjectCreateEvent )
    ReportingS3ObjectCreateEvent event = persisted
    assertEquals( "Persisted event bucket name", "bucket15", event.getS3BucketName() )
    assertEquals( "Persisted event object name", "object34", event.getS3ObjectKey() )
    assertEquals( "Persisted event object version", "version1", event.getObjectVersion() )
    assertEquals( "Persisted event size", Integer.MAX_VALUE.toLong() + 1L, event.getSize() )
    assertEquals( "Persisted event user id", Principals.systemFullName().getUserId(), event.getUserId() )
    assertEquals( "Persisted event timestamp", timestamp, event.getTimestampMs() )
  }

  @Test
  void testDeleteEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( S3ObjectEvent.with(
        S3ObjectEvent.forS3ObjectDelete(),
        "bucket15",
        "object34",
        null,
        Principals.systemFullName().getUserId(),
        Integer.MAX_VALUE.toLong() + 1L
    ), timestamp )

    assertTrue( "Persisted event is ReportingS3BucketDeleteEvent", persisted instanceof ReportingS3ObjectDeleteEvent )
    ReportingS3ObjectDeleteEvent event = persisted
    assertEquals( "Persisted event bucket name", "bucket15", event.getS3BucketName() )
    assertEquals( "Persisted event object name", "object34", event.getS3ObjectKey() )
    assertNull( "Persisted event object version", event.getObjectVersion() )
    assertEquals( "Persisted event timestamp", timestamp, event.getTimestampMs() )
  }

  @Test
  void testNullVersionEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( S3ObjectEvent.with(
        S3ObjectEvent.forS3ObjectDelete(),
        "bucket15",
        "object34",
        WalrusProperties.NULL_VERSION_ID,
        Principals.systemFullName().getUserId(),
        Integer.MAX_VALUE.toLong() + 1L
    ), timestamp )

    assertTrue( "Persisted event is ReportingS3BucketDeleteEvent", persisted instanceof ReportingS3ObjectDeleteEvent )
    ReportingS3ObjectDeleteEvent event = persisted
    assertEquals( "Persisted event bucket name", "bucket15", event.getS3BucketName() )
    assertEquals( "Persisted event object name", "object34", event.getS3ObjectKey() )
    assertNull( "Persisted event object version", event.getObjectVersion() )
    assertEquals( "Persisted event timestamp", timestamp, event.getTimestampMs() )
  }

  private Object testEvent( S3ObjectEvent event, long timestamp ) {
    String updatedAccountId = null
    String updatedAccountName = null
    String updatedUserId = null
    String updatedUserName = null
    Object persisted = null
    ReportingAccountCrud accountCrud = new ReportingAccountCrud( ) {
      @Override void createOrUpdateAccount( String id, String name ) {
        updatedAccountId = id
        updatedAccountName = name
      }
    }
    ReportingUserCrud userCrud = new ReportingUserCrud( ) {
      @Override void createOrUpdateUser( String id, String accountId, String name ) {
        updatedUserId = id
        updatedUserName = name
      }
    }
    ReportingS3ObjectEventStore eventStore = new ReportingS3ObjectEventStore( ) {
      @Override protected void persist( final Object o ) {
        persisted = o
      }
    }
    S3ObjectUsageEventListener listener = new S3ObjectUsageEventListener( ) {
      @Override protected ReportingAccountCrud getReportingAccountCrud() { return accountCrud }
      @Override protected ReportingUserCrud getReportingUserCrud() { return userCrud }
      @Override protected ReportingS3ObjectEventStore getReportingS3ObjectEventStore() { eventStore }
      @Override protected long getCurrentTimeMillis() { timestamp }
      @Override protected User lookupUser( final String userId ) {
        assertEquals( "Looked up user", "eucalyptus", userId )
        Principals.systemUser()
      }
    }

    listener.fireEvent( event )

    assertNotNull( "Persisted event", persisted )
    assertEquals( "Account Id", "000000000000", updatedAccountId  )
    assertEquals( "Account Name", "eucalyptus", updatedAccountName )
    assertEquals( "User Id", "eucalyptus", updatedUserId )
    assertEquals( "User Name", "eucalyptus", updatedUserName )

    persisted
  }
}
