package com.eucalyptus.reporting.modules.storage

import org.junit.Test
import com.eucalyptus.auth.principal.Principals

import static org.junit.Assert.*
import com.eucalyptus.reporting.domain.ReportingAccountCrud
import com.eucalyptus.reporting.domain.ReportingUserCrud
import com.eucalyptus.auth.principal.User
import com.google.common.base.Charsets
import com.eucalyptus.reporting.event.SnapShotEvent
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotEventStore
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotCreateEvent
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotDeleteEvent

/**
 * 
 */
class SnapShotUsageEventListenerTest {

  @Test
  void testInstantiable() {
    new SnapShotUsageEventListener()
  }

  @Test
  void testCreateEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( SnapShotEvent.with(
        SnapShotEvent.forSnapShotCreate(Integer.MAX_VALUE.toLong() + 1L, uuid("vol-00000001"), "vol-00000001"),
        uuid("snap-00000001"),
        "snap-00000001",
        Principals.systemFullName().getUserId()
    ), timestamp )

    assertTrue( "Persisted event is ReportingVolumeSnapshotCreateEvent", persisted instanceof ReportingVolumeSnapshotCreateEvent )
    ReportingVolumeSnapshotCreateEvent event = persisted
    assertEquals( "Persisted event uuid", uuid("snap-00000001"), event.getUuid() )
    assertEquals( "Persisted event name", "snap-00000001", event.getVolumeSnapshotId() )
    assertEquals( "Persisted event size", Integer.MAX_VALUE.toLong() + 1L, event.getSizeGB() )
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
        Principals.systemFullName().getUserId()
    ), timestamp )

    assertTrue( "Persisted event is ReportingVolumeSnapshotDeleteEvent", persisted instanceof ReportingVolumeSnapshotDeleteEvent )
    ReportingVolumeSnapshotDeleteEvent event = persisted
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
      @Override protected User lookupUser( final String userId ) {
        assertEquals( "Looked up user", "eucalyptus", userId )
        Principals.systemUser()
      }
    }

    listener.fireEvent( event )

    assertNotNull( "Persisted event", persisted )
    assertEquals( "Account Id", "eucalyptus", updatedAccountId  )
    assertEquals( "Account Name", "000000000000", updatedAccountName )
    assertEquals( "User Id", "eucalyptus", updatedUserId )
    assertEquals( "User Name", "eucalyptus", updatedUserName )

    persisted
  }

  private String uuid( String seed ) {
    return UUID.nameUUIDFromBytes( seed.getBytes(Charsets.UTF_8) ).toString()
  }
}
