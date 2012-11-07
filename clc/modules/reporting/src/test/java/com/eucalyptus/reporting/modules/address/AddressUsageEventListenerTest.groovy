package com.eucalyptus.reporting.modules.address

import com.eucalyptus.reporting.domain.ReportingAccountCrud
import com.eucalyptus.reporting.domain.ReportingUserCrud
import com.eucalyptus.reporting.event_store.ReportingElasticIpEventStore
import com.eucalyptus.reporting.event_store.ReportingElasticIpCreateEvent
import com.eucalyptus.reporting.event_store.ReportingElasticIpDeleteEvent
import com.eucalyptus.reporting.event_store.ReportingElasticIpAttachEvent
import com.eucalyptus.reporting.event_store.ReportingElasticIpDetachEvent
import com.google.common.base.Charsets
import com.eucalyptus.reporting.event.AddressEvent
import com.eucalyptus.auth.principal.Principals

import static org.junit.Assert.*
import org.junit.Test

/**
 * Unit test for AddressUsageEventListener
 */
class AddressUsageEventListenerTest {

  @Test
  void testInstantiable() {
    new AddressUsageEventListener()
  }

  @Test
  void testAllocateEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( AddressEvent.with(
        "127.0.0.1",
        Principals.systemFullName(),
        "testaccount",
        AddressEvent.forAllocate()
    ), timestamp )

    assertTrue( "Persisted event is ReportingElasticIpCreateEvent", persisted instanceof ReportingElasticIpCreateEvent )
    ReportingElasticIpCreateEvent event = persisted
    assertEquals( "Event user id", "eucalyptus", event.getUserId() )
    assertEquals( "Event ip", "127.0.0.1", event.getIp() )
    assertEquals( "Event timestamp", timestamp, event.getTimestampMs() )
  }

  @Test
  void testReleaseEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( AddressEvent.with(
        "127.0.0.1",
        Principals.systemFullName(),
        "testaccount",
        AddressEvent.forRelease()
    ), timestamp )

    assertTrue( "Persisted event is ReportingElasticIpDeleteEvent", persisted instanceof ReportingElasticIpDeleteEvent )
    ReportingElasticIpDeleteEvent event = persisted
    assertEquals( "Event address uuid", "127.0.0.1", event.getIp() )
    assertEquals( "Event timestamp", timestamp, event.getTimestampMs() )
  }

  @Test
  void testAssociateEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( AddressEvent.with(
        "127.0.0.1",
        Principals.systemFullName(),
        "testaccount",
        AddressEvent.forAssociate(uuid("instance"), "i-12345678")
    ), timestamp )

    assertTrue( "Persisted event is ReportingElasticIpAttachEvent", persisted instanceof ReportingElasticIpAttachEvent )
    ReportingElasticIpAttachEvent event = persisted
    assertEquals( "Event address uuid", "127.0.0.1", event.getIp() )
    assertEquals( "Event instance uuid", uuid("instance"), event.getInstanceUuid() )
    assertEquals( "Event timestamp", timestamp, event.getTimestampMs() )
  }

  @Test
  void testDisassociateEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( AddressEvent.with(
        "127.0.0.1",
        Principals.systemFullName(),
        "testaccount",
        AddressEvent.forDisassociate(uuid("instance2"), "i-12345678")
    ), timestamp )

    assertTrue( "Persisted event is ReportingElasticIpDetachEvent", persisted instanceof ReportingElasticIpDetachEvent )
    ReportingElasticIpDetachEvent event = persisted
    assertEquals( "Event address ip", "127.0.0.1", event.getIp() )
    assertEquals( "Event instance uuid", uuid("instance2"), event.getInstanceUuid() )
    assertEquals( "Event timestamp", timestamp, event.getTimestampMs() )
  }

  private Object testEvent( AddressEvent event, long timestamp ) {
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
    ReportingElasticIpEventStore eventStore = new ReportingElasticIpEventStore( ) {
      @Override protected void persist( final Object o ) {
        persisted = o
      }
    }
    AddressUsageEventListener listener = new AddressUsageEventListener( ) {
      @Override protected ReportingAccountCrud getReportingAccountCrud() { return accountCrud }
      @Override protected ReportingUserCrud getReportingUserCrud() { return userCrud }
      @Override protected ReportingElasticIpEventStore getReportingElasticIpEventStore() { eventStore }
      @Override protected long getCurrentTimeMillis() { timestamp }
    }

    listener.fireEvent( event )

    assertNotNull( "Persisted event", persisted )
    assertEquals( "Account Id", "000000000000", updatedAccountId  )
    assertEquals( "Account Name", "testaccount", updatedAccountName )
    assertEquals( "User Id", "eucalyptus", updatedUserId )
    assertEquals( "User Name", "eucalyptus", updatedUserName )

    persisted
  }

  private String uuid( String seed ) {
    return UUID.nameUUIDFromBytes( seed.getBytes(Charsets.UTF_8) ).toString()
  }
}
