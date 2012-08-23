package com.eucalyptus.reporting.modules.instance

import com.eucalyptus.reporting.domain.ReportingAccountCrud
import com.eucalyptus.reporting.domain.ReportingUserCrud
import com.eucalyptus.reporting.event.InstanceEvent
import com.eucalyptus.reporting.event_store.ReportingInstanceEventStore

import static org.junit.Assert.*
import org.junit.Test
import com.google.common.base.Charsets
import com.google.common.collect.Lists
import com.google.common.base.Function
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent
import com.eucalyptus.reporting.event_store.ReportingInstanceUsageEvent
import com.google.common.collect.MapMaker
import java.util.concurrent.TimeUnit
import com.google.common.collect.ImmutableList

/**
 * Unit test for InstanceEventListener
 */
class InstanceEventListenerTest {
  Reference<Long> timestamp = new Reference<Long>()
  Reference<String> updatedAccountId = new Reference<String>()
  Reference<String> updatedAccountName = new Reference<String>()
  Reference<String> updatedUserId = new Reference<String>()
  Reference<String> updatedUserName = new Reference<String>()
  List<Object> persisted = Lists.newArrayList()
  ReportingAccountCrud accountDao = new ReportingAccountCrud( ) {
    @Override void createOrUpdateAccount( String id, String name ) {
      updatedAccountId.set( id )
      updatedAccountName.set( name )
    }
  }
  ReportingUserCrud userDao = new ReportingUserCrud( ) {
    @Override void createOrUpdateUser( String id, String accountId, String name ) {
      updatedUserId.set( id )
      updatedUserName.set( name )
    }
  }
  ReportingInstanceEventStore eventStore = new ReportingInstanceEventStore( ) {
    @Override protected void persist( Object reportingEvent ) {
      persisted.add( reportingEvent )
    }
  }
  InstanceEventListener listener = new InstanceEventListener( ) {
    @Override protected ReportingAccountCrud getReportingAccountCrud() { return accountDao }
    @Override protected ReportingUserCrud getReportingUserCrud() { return userDao }
    @Override protected ReportingInstanceEventStore getReportingInstanceEventStore() { eventStore }
    @Override protected long getCurrentTimeMillis() { timestamp.get() }
    @Override protected def <P, R> Function<P, R> transactional( final Class<?> clazz,
                                                                 final Function<P, R> callback) {
      return callback
    }
    @Override protected MapMaker getExpiringMapMaker() {
      return new MapMaker().expireAfterAccess( 1, TimeUnit.SECONDS );
    }
  }

  @Test
  void testInstantiable() {
    new InstanceEventListener()
  }

  @Test
  void testRunNewInstance() {
    long timestamp = System.currentTimeMillis() - 10000
    List<Object> persisted = testEvent( newInstanceEvent("i-00000001"), timestamp )

    assertEquals( "Persisted event count", 2, persisted.size() )
    assertTrue( "Persisted event 0 is ReportingInstanceCreateEvent", persisted.get(0) instanceof ReportingInstanceCreateEvent )
    assertTrue( "Persisted event 1 is ReportingInstanceUsageEvent", persisted.get(1) instanceof ReportingInstanceUsageEvent )
    ReportingInstanceCreateEvent createEvent = (ReportingInstanceCreateEvent) persisted.get(0)
    ReportingInstanceUsageEvent usageEvent = (ReportingInstanceUsageEvent) persisted.get(1)
    assertEquals( "Create event instance uuid", uuid("i-00000001"), createEvent.uuid )
    assertEquals( "Create event instance id", "i-00000001", createEvent.instanceId )
    assertEquals( "Create event timestamp", timestamp, createEvent.timestampMs )
    assertEquals( "Create event instance type", "c1.medium", createEvent.instanceType )
    assertEquals( "Create event user id", "1234", createEvent.userId )
    assertEquals( "Create event cluster name", "CC_123", createEvent.clusterName )
    assertEquals( "Create event availability zone", "PARTI00", createEvent.availabilityZone )

    // Regular invocation failing due to mismatch between object type and groovy metaclass for ReportingInstanceUsageEvent (groovy 1.7.2)
    assertEquals( "Usage event instance uuid", uuid("i-00000001"), invoke(usageEvent,"getUuid") )
    assertEquals( "Usage event timestamp", timestamp, invoke(usageEvent,"getTimestampMs") )
    assertEquals( "Usage event cumulativeDiskIoMegs", 20, invoke(usageEvent,"getCumulativeDiskIoMegs") )
    assertEquals( "Usage event cpuUtilizationPercent", 85, invoke(usageEvent,"getCpuUtilizationPercent") )
    assertEquals( "Usage event getCumulativeNetIncomingMegsBetweenZones", 1, invoke(usageEvent,"getCumulativeNetIncomingMegsBetweenZones") )
    assertEquals( "Usage event getCumulativeNetIncomingMegsWithinZone", 2, invoke(usageEvent,"getCumulativeNetIncomingMegsWithinZone") )
    assertEquals( "Usage event getCumulativeNetIncomingMegsPublic", 3, invoke(usageEvent,"getCumulativeNetIncomingMegsPublic") )
    assertEquals( "Usage event getCumulativeNetOutgoingMegsBetweenZones", 4, invoke(usageEvent,"getCumulativeNetOutgoingMegsBetweenZones") )
    assertEquals( "Usage event getCumulativeNetOutgoingMegsWithinZone", 5, invoke(usageEvent,"getCumulativeNetOutgoingMegsWithinZone") )
    assertEquals( "Usage event getCumulativeNetOutgoingMegsPublic", 6, invoke(usageEvent,"getCumulativeNetOutgoingMegsPublic") )
  }

  @Test
  void testInstanceUsage() {
    long timestamp = System.currentTimeMillis() + ( 1200 * 1000 )
    List<Object> persisted1 = testEvent( newInstanceEvent("i-00000002"), timestamp )
    List<Object> persisted2 = testEvent( newInstanceEvent("i-00000002"), timestamp + 10 )
    Thread.sleep( 1000L )
    List<Object> persisted3 = testEvent( newInstanceEvent("i-00000002"), timestamp + 20 )
    List<Object> persisted4 = testEvent( newInstanceEvent("i-00000003"), timestamp + ( 1201 * 1000 ) )

    assertEquals( "Persisted event count 1", 2, persisted1.size() ) // Creation and usage
    assertEquals( "Persisted event count 2", 0, persisted2.size() ) // None, already created and not due for usage persistence
    assertEquals( "Persisted event count 3", 1, persisted3.size() ) // Creation, due to timeout (set to 1 sec for test)
    assertEquals( "Persisted event count 4", 3, persisted4.size() ) // Creation i-*3 and usage for i-*2 and i-*3

  }

  private InstanceEvent newInstanceEvent( String instanceId ) {
    new InstanceEvent(
        uuid(instanceId),
        instanceId,
        "c1.medium",
        "1234",
        "testuser",
        "5678",
        "testaccount",
        "CC_123",
        "PARTI00",
        20, // Disk
        85, // CPU
        1, 2, 3, 4, 5, 6 // Network usage
    )
  }

  private Object invoke( Object object, String method ) {
    object.getClass().getMethod( method ).invoke( object )
  }

  private List<Object> testEvent( InstanceEvent event, long timestamp ) {
    this.timestamp.set( timestamp )
    this.updatedAccountId.set( null )
    this.updatedAccountName.set( null )
    this.updatedUserId.set( null )
    this.updatedUserName.set( null )
    this.persisted.clear()

    listener.fireEvent( event )

    assertNotNull( "Persisted event", persisted )
    assertEquals( "Account Id", "5678", updatedAccountId.get() )
    assertEquals( "Account Name", "testaccount", updatedAccountName.get() )
    assertEquals( "User Id", "1234", updatedUserId.get() )
    assertEquals( "User Name", "testuser", updatedUserName.get() )

    ImmutableList.copyOf( persisted )
  }

  private String uuid( String seed ) {
    return UUID.nameUUIDFromBytes( seed.getBytes(Charsets.UTF_8) ).toString()
  }
}
