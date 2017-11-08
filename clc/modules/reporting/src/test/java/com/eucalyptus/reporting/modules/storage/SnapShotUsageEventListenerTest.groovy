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
package com.eucalyptus.reporting.modules.storage

import com.eucalyptus.auth.AuthException
import com.eucalyptus.reporting.service.ReportingService
import groovy.transform.CompileStatic
import org.junit.BeforeClass
import org.junit.Test
import com.eucalyptus.auth.principal.Principals

import static org.junit.Assert.*
import com.eucalyptus.reporting.domain.ReportingAccountCrud
import com.eucalyptus.reporting.domain.ReportingUserCrud
import com.google.common.base.Charsets
import com.eucalyptus.reporting.event.SnapShotEvent
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotEventStore
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotCreateEvent
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotDeleteEvent

/**
 * 
 */
@CompileStatic
class SnapShotUsageEventListenerTest {

  @BeforeClass
  static void beforeClass( ) {
    ReportingService.DATA_COLLECTION_ENABLED = true
  }

  @Test
  void testInstantiable() {
    new SnapShotUsageEventListener()
  }

  @Test
  void testCreateEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( SnapShotEvent.with(
        SnapShotEvent.forSnapShotCreate(Integer.MAX_VALUE, uuid("vol-00000001"), "vol-00000001"),
        uuid("snap-00000001"),
        "snap-00000001",
        Principals.systemFullName().getUserId(),
        Principals.systemFullName().getUserName(),
        Principals.systemFullName().getAccountNumber()
    ), timestamp )

    assertTrue( "Persisted event is ReportingVolumeSnapshotCreateEvent", persisted instanceof ReportingVolumeSnapshotCreateEvent )
    ReportingVolumeSnapshotCreateEvent event = (ReportingVolumeSnapshotCreateEvent) persisted
    assertEquals( "Persisted event uuid", uuid("snap-00000001"), event.getUuid() )
    assertEquals( "Persisted event name", "snap-00000001", event.getVolumeSnapshotId() )
    assertEquals( "Persisted event size", Integer.MAX_VALUE, event.getSizeGB() )
    assertEquals( "Persisted event user id", Principals.systemFullName().getUserId(), event.getUserId() )
    assertEquals( "Persisted event timestamp", timestamp, event.getTimestampMs() )
  }

  @Test
  void testDeleteEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( SnapShotEvent.with(
        SnapShotEvent.forSnapShotDelete(),
        uuid("snap-00000001"),
        "snap-00000001",
        Principals.systemFullName().getUserId(),
        Principals.systemFullName().getUserName(),
        Principals.systemFullName().getAccountNumber()
    ), timestamp )

    assertTrue( "Persisted event is ReportingVolumeSnapshotDeleteEvent", persisted instanceof ReportingVolumeSnapshotDeleteEvent )
    ReportingVolumeSnapshotDeleteEvent event = (ReportingVolumeSnapshotDeleteEvent) persisted
    assertEquals( "Persisted event uuid", uuid("snap-00000001"), event.getUuid() )
    assertEquals( "Persisted event timestamp", timestamp, event.getTimestampMs() )
  }

  private Object testEvent( SnapShotEvent event, long timestamp ) {
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
    ReportingVolumeSnapshotEventStore eventStore = new ReportingVolumeSnapshotEventStore( ) {
      @Override protected void persist( final Object o ) {
        persisted = o
      }
    }
    SnapShotUsageEventListener listener = new SnapShotUsageEventListener( ) {
      @Override protected ReportingAccountCrud getReportingAccountCrud() { return accountCrud }
      @Override protected ReportingUserCrud getReportingUserCrud() { return userCrud }
      @Override protected ReportingVolumeSnapshotEventStore getReportingVolumeSnapshotEventStore() { eventStore }
      @Override protected long getCurrentTimeMillis() { timestamp }
      @Override protected String lookupAccountAliasById(final String accountNumber) throws AuthException {
        assertEquals( "Account Id", "000000000000", accountNumber  )
        'eucalyptus'
      }
    }

    listener.fireEvent( event )

    assertNotNull( "Persisted event", persisted )
    assertEquals( "Account Id","000000000000" , updatedAccountId  )
    assertEquals( "Account Name", "eucalyptus", updatedAccountName )
    assertEquals( "User Id", "eucalyptus", updatedUserId )
    assertEquals( "User Name", "eucalyptus", updatedUserName )

    persisted
  }

  private String uuid( String seed ) {
    return UUID.nameUUIDFromBytes( seed.getBytes(Charsets.UTF_8) ).toString()
  }
}
