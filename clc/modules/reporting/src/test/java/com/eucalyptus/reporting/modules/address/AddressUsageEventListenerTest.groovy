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
package com.eucalyptus.reporting.modules.address

import com.eucalyptus.reporting.domain.ReportingAccountCrud
import com.eucalyptus.reporting.domain.ReportingUserCrud
import com.eucalyptus.reporting.event.AddressEvent
import com.eucalyptus.reporting.event_store.ReportingElasticIpEventStore
import com.eucalyptus.reporting.event_store.ReportingElasticIpCreateEvent
import com.eucalyptus.reporting.event_store.ReportingElasticIpDeleteEvent
import com.eucalyptus.reporting.event_store.ReportingElasticIpAttachEvent
import com.eucalyptus.reporting.event_store.ReportingElasticIpDetachEvent
import com.eucalyptus.reporting.service.ReportingService
import com.eucalyptus.auth.principal.OwnerFullName
import com.google.common.base.Charsets
import groovy.transform.CompileStatic
import org.junit.BeforeClass

import static org.junit.Assert.*
import org.junit.Test

/**
 * Unit test for AddressUsageEventListener
 */
@CompileStatic
class AddressUsageEventListenerTest {

  @BeforeClass
  static void beforeClass( ) {
    ReportingService.DATA_COLLECTION_ENABLED = true
  }

  @Test
  void testInstantiable() {
    new AddressUsageEventListener()
  }

  @Test
  void testAllocateEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( AddressEvent.with(
        "127.0.0.1",
        userFullName( "testaccount" ),
        "testaccount",
        AddressEvent.forAllocate()
    ), timestamp )

    assertTrue( "Persisted event is ReportingElasticIpCreateEvent", persisted instanceof ReportingElasticIpCreateEvent )
    ReportingElasticIpCreateEvent event = (ReportingElasticIpCreateEvent) persisted
    assertEquals( "Event user id", "testaccount", event.getUserId() )
    assertEquals( "Event ip", "127.0.0.1", event.getIp() )
    assertEquals( "Event timestamp", timestamp, event.getTimestampMs() )
  }

  @Test
  void testReleaseEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( AddressEvent.with(
        "127.0.0.1",
        userFullName( "testaccount" ),
        "testaccount",
        AddressEvent.forRelease()
    ), timestamp )

    assertTrue( "Persisted event is ReportingElasticIpDeleteEvent", persisted instanceof ReportingElasticIpDeleteEvent )
    ReportingElasticIpDeleteEvent event = (ReportingElasticIpDeleteEvent) persisted
    assertEquals( "Event address uuid", "127.0.0.1", event.getIp() )
    assertEquals( "Event timestamp", timestamp, event.getTimestampMs() )
  }

  @Test
  void testAssociateEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( AddressEvent.with(
        "127.0.0.1",
        userFullName( "testaccount" ),
        "testaccount",
        AddressEvent.forAssociate(uuid("instance"), "i-12345678")
    ), timestamp )

    assertTrue( "Persisted event is ReportingElasticIpAttachEvent", persisted instanceof ReportingElasticIpAttachEvent )
    ReportingElasticIpAttachEvent event = (ReportingElasticIpAttachEvent) persisted
    assertEquals( "Event address uuid", "127.0.0.1", event.getIp() )
    assertEquals( "Event instance uuid", uuid("instance"), event.getInstanceUuid() )
    assertEquals( "Event timestamp", timestamp, event.getTimestampMs() )
  }

  @Test
  void testDisassociateEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( AddressEvent.with(
        "127.0.0.1",
        userFullName( "testaccount" ),
        "testaccount",
        AddressEvent.forDisassociate(uuid("instance2"), "i-12345678")
    ), timestamp )

    assertTrue( "Persisted event is ReportingElasticIpDetachEvent", persisted instanceof ReportingElasticIpDetachEvent )
    ReportingElasticIpDetachEvent event = (ReportingElasticIpDetachEvent) persisted
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
    assertEquals( "Account Id", "100000000000", updatedAccountId  )
    assertEquals( "Account Name", "testaccount", updatedAccountName )
    assertEquals( "User Id", "testaccount", updatedUserId )
    assertEquals( "User Name", "testaccount", updatedUserName )

    persisted
  }

  private String uuid( String seed ) {
    return UUID.nameUUIDFromBytes( seed.getBytes(Charsets.UTF_8) ).toString()
  }

  private OwnerFullName userFullName( final String name ) {
    return new OwnerFullName() {
      @Override public String getAccountNumber() { return "100000000000";  }
      @Override public String getUserId() { return name; }
      @Override public String getUserName() { return name; }
      @Override public boolean isOwner( final String ownerId ) { return false; }
      @Override public boolean isOwner( final OwnerFullName ownerFullName ) { return false; }
      @Override public String getUniqueId() { return null; }
      @Override public String getVendor() { return null; }
      @Override public String getRegion() { return null; }
      @Override public String getNamespace() { return null; }
      @Override public String getAuthority() { return null; }
      @Override public String getRelativeId() { return null; }
      @Override public String getPartition() { return null; }
    };
  }
}
