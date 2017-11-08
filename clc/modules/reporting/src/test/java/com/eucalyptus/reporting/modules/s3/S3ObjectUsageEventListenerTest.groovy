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
package com.eucalyptus.reporting.modules.s3

import com.eucalyptus.auth.AuthException
import com.eucalyptus.reporting.service.ReportingService
import groovy.transform.CompileStatic
import org.junit.BeforeClass
import org.junit.Test
import com.eucalyptus.auth.principal.Principals

import static org.junit.Assert.*
import com.eucalyptus.reporting.domain.ReportingAccountCrud
import com.eucalyptus.reporting.domain.ReportingUserCrud
import com.eucalyptus.reporting.event.S3ObjectEvent
import com.eucalyptus.reporting.event_store.ReportingS3ObjectEventStore
import com.eucalyptus.reporting.event_store.ReportingS3ObjectCreateEvent
import com.eucalyptus.reporting.event_store.ReportingS3ObjectDeleteEvent

/**
 * 
 */
@CompileStatic
class S3ObjectUsageEventListenerTest {

  @BeforeClass
  static void beforeClass( ) {
    ReportingService.DATA_COLLECTION_ENABLED = true
  }

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
        Principals.systemFullName().getUserName(),
        Principals.systemFullName().getAccountNumber(),
        Integer.MAX_VALUE.toLong() + 1L
    ), timestamp )

    assertTrue( "Persisted event is ReportingS3BucketCreateEvent", persisted instanceof ReportingS3ObjectCreateEvent )
    ReportingS3ObjectCreateEvent event = (ReportingS3ObjectCreateEvent) persisted
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
        Principals.systemFullName().getUserName(),
        Principals.systemFullName().getAccountNumber(),
        Integer.MAX_VALUE.toLong() + 1L
    ), timestamp )

    assertTrue( "Persisted event is ReportingS3BucketDeleteEvent", persisted instanceof ReportingS3ObjectDeleteEvent )
    ReportingS3ObjectDeleteEvent event = (ReportingS3ObjectDeleteEvent) persisted
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
        "null",
        Principals.systemFullName().getUserId(),
        Principals.systemFullName().getUserName(),
        Principals.systemFullName().getAccountNumber(),
        Integer.MAX_VALUE.toLong() + 1L
    ), timestamp )

    assertTrue( "Persisted event is ReportingS3BucketDeleteEvent", persisted instanceof ReportingS3ObjectDeleteEvent )
    ReportingS3ObjectDeleteEvent event = (ReportingS3ObjectDeleteEvent) persisted
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
      @Override protected String lookupAccountAliasById(final String accountNumber) throws AuthException {
        assertEquals( "Account Id", "000000000000", accountNumber  )
        'eucalyptus'
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
