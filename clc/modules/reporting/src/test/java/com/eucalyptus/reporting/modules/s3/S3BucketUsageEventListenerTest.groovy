package com.eucalyptus.reporting.modules.s3

import org.junit.Test
import com.eucalyptus.auth.principal.Principals

import static org.junit.Assert.*
import com.eucalyptus.reporting.domain.ReportingAccountCrud
import com.eucalyptus.reporting.domain.ReportingUserCrud
import com.google.common.base.Charsets
import com.eucalyptus.reporting.event.S3BucketEvent
import com.eucalyptus.reporting.event_store.ReportingS3BucketEventStore
import com.eucalyptus.reporting.event_store.ReportingS3BucketCreateEvent
import com.eucalyptus.reporting.event_store.ReportingS3BucketDeleteEvent
import com.eucalyptus.auth.principal.User

/**
 * Unit test for S3BucketUsageEventListener
 */
class S3BucketUsageEventListenerTest {

  @Test
  void testInstantiable() {
    new S3BucketUsageEventListener()
  }

  @Test
  void testCreateEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( S3BucketEvent.with(
        S3BucketEvent.forS3BucketCreate(),
        uuid("bucket15"),
        "bucket15",
        Principals.systemFullName(),
        Integer.MAX_VALUE.toLong() + 1L
    ), timestamp )

    assertTrue( "Persisted event is ReportingS3BucketCreateEvent", persisted instanceof ReportingS3BucketCreateEvent )
    ReportingS3BucketCreateEvent event = persisted
    assertEquals( "Persisted event name", "bucket15", event.getS3BucketName() )
    assertEquals( "Persisted event size", Integer.MAX_VALUE.toLong() + 1L, event.getBucketSize() )
    assertEquals( "Persisted event user id", Principals.systemFullName().getUserId(), event.getUserId() )
    assertEquals( "Persisted event timestamp", timestamp, event.getTimeInMs() )
  }

  @Test
  void testDeleteEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( S3BucketEvent.with(
        S3BucketEvent.forS3BucketDelete(),
        uuid("bucket15"),
        "bucket15",
        Principals.systemFullName(),
        Integer.MAX_VALUE.toLong() + 1L
    ), timestamp )

    assertTrue( "Persisted event is ReportingS3BucketDeleteEvent", persisted instanceof ReportingS3BucketDeleteEvent )
    ReportingS3BucketDeleteEvent event = persisted
    assertEquals( "Persisted event name", "bucket15", event.getS3BucketName() )
    assertEquals( "Persisted event size", Integer.MAX_VALUE.toLong() + 1L, event.getS3BucketSize() )
    assertEquals( "Persisted event user id", Principals.systemFullName().getUserId(), event.getS3userId() )
    assertEquals( "Persisted event timestamp", timestamp, event.getTimestampMs() )
  }

  private Object testEvent( S3BucketEvent event, long timestamp ) {
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
    ReportingS3BucketEventStore eventStore = new ReportingS3BucketEventStore( ) {
      @Override protected void persist( final Object o ) {
        persisted = o
      }
    }
    S3BucketUsageEventListener listener = new S3BucketUsageEventListener( ) {
      @Override protected ReportingAccountCrud getReportingAccountCrud() { return accountCrud }
      @Override protected ReportingUserCrud getReportingUserCrud() { return userCrud }
      @Override protected ReportingS3BucketEventStore getReportingS3BucketEventStore() { eventStore }
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
