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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/
package com.eucalyptus.reporting.event;

import java.util.Arrays;
import java.util.UUID;
import static org.junit.Assert.*;
import static com.eucalyptus.reporting.event.AddressEvent.ActionInfo;
import static com.eucalyptus.reporting.event.AddressEvent.AddressAction;
import static com.eucalyptus.reporting.event.AddressEvent.InstanceActionInfo;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Availability;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Dimension;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Tag;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Type;
import org.junit.Test;
import com.eucalyptus.auth.principal.Principals;
import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;

/**
 * Unit tests for event classes
 */
public class ReportingEventTest {

  @Test
  public void testAddressAllocateEventCreation() {
    final AddressEvent event = AddressEvent.with(
        uuid("test"),
        "127.0.0.1",
        Principals.systemFullName(),
        "testaccount",
        AddressEvent.forAllocate()
        );

    assertEquals( "uuid", uuid("test"), event.getUuid() );
    assertEquals( "address", "127.0.0.1", event.getAddress() );
    assertEquals( "account number", Principals.systemFullName().getAccountNumber(), event.getAccountId() );
    assertEquals( "account name", "testaccount", event.getAccountName() );
    assertEquals( "user number", Principals.systemFullName().getUserId(), event.getUserId() );
    assertEquals( "user name", Principals.systemFullName().getUserName(), event.getUserName() );
    assertEquals( "action info type", ActionInfo.class, event.getActionInfo().getClass() );
    assertEquals( "action info type", AddressAction.ALLOCATE, event.getActionInfo().getAction() );
    assertEquals( "event string", "[address:127.0.0.1,userId:eucalyptus,accountId:000000000000,actionInfo:[action:ALLOCATE]]", event.toString() );
  }

  @Test
  public void testAddressReleaseEventCreation() {
    final AddressEvent event = AddressEvent.with(
        uuid("test"),
        "127.0.0.1",
        Principals.systemFullName(),
        "testaccount",
        AddressEvent.forRelease()
    );

    assertEquals( "uuid", uuid("test"), event.getUuid() );
    assertEquals( "address", "127.0.0.1", event.getAddress() );
    assertEquals( "account number", Principals.systemFullName().getAccountNumber(), event.getAccountId() );
    assertEquals( "account name", "testaccount", event.getAccountName() );
    assertEquals( "user number", Principals.systemFullName().getUserId(), event.getUserId() );
    assertEquals( "user name", Principals.systemFullName().getUserName(), event.getUserName() );
    assertEquals( "action info type", ActionInfo.class, event.getActionInfo().getClass() );
    assertEquals( "action info type", AddressAction.RELEASE, event.getActionInfo().getAction() );
    assertEquals( "event string", "[address:127.0.0.1,userId:eucalyptus,accountId:000000000000,actionInfo:[action:RELEASE]]", event.toString() );
  }

  @Test
  public void testAddressAssociateEventCreation() {
    final AddressEvent event = AddressEvent.with(
        uuid("test"),
        "127.0.0.1",
        Principals.systemFullName(),
        "testaccount",
        AddressEvent.forAssociate(uuid("instance"), "i-12345678")
    );

    assertEquals( "uuid", uuid("test"), event.getUuid() );
    assertEquals( "address", "127.0.0.1", event.getAddress() );
    assertEquals( "account number", Principals.systemFullName().getAccountNumber(), event.getAccountId() );
    assertEquals( "account name", "testaccount", event.getAccountName() );
    assertEquals( "user number", Principals.systemFullName().getUserId(), event.getUserId() );
    assertEquals( "user name", Principals.systemFullName().getUserName(), event.getUserName() );
    assertEquals( "action info type", InstanceActionInfo.class, event.getActionInfo().getClass() );
    assertEquals( "action info type", AddressAction.ASSOCIATE, event.getActionInfo().getAction() );
    assertEquals( "action info type", uuid("instance"), ((InstanceActionInfo)event.getActionInfo()).getInstanceUuid() );
    assertEquals( "action info type", "i-12345678", ((InstanceActionInfo)event.getActionInfo()).getInstanceId() );
    assertEquals( "event string", "[address:127.0.0.1,userId:eucalyptus,accountId:000000000000,actionInfo:[action:ASSOCIATE,instanceUuid:7123a699-d77d-3647-9a1d-8ece2c4f1c16,instanceId:i-12345678]]", event.toString() );
  }

  @Test
  public void testAddressDisassociateEventCreation() {
    final AddressEvent event = AddressEvent.with(
        uuid("test"),
        "127.0.0.1",
        Principals.systemFullName(),
        "testaccount",
        AddressEvent.forDisassociate(uuid("instance2"), "i-12345678")
    );

    assertEquals( "uuid", uuid("test"), event.getUuid() );
    assertEquals( "address", "127.0.0.1", event.getAddress() );
    assertEquals( "account number", Principals.systemFullName().getAccountNumber(), event.getAccountId() );
    assertEquals( "account name", "testaccount", event.getAccountName() );
    assertEquals( "user number", Principals.systemFullName().getUserId(), event.getUserId() );
    assertEquals( "user name", Principals.systemFullName().getUserName(), event.getUserName() );
    assertEquals( "action info type", InstanceActionInfo.class, event.getActionInfo().getClass() );
    assertEquals( "action info type", AddressAction.DISASSOCIATE, event.getActionInfo().getAction() );
    assertEquals( "action info type", uuid("instance2"), ((InstanceActionInfo)event.getActionInfo()).getInstanceUuid() );
    assertEquals( "action info type", "i-12345678", ((InstanceActionInfo)event.getActionInfo()).getInstanceId() );
    assertEquals( "event string", "[address:127.0.0.1,userId:eucalyptus,accountId:000000000000,actionInfo:[action:DISASSOCIATE,instanceUuid:781e6aca-85f8-3f45-9e1f-58d2fb126bca,instanceId:i-12345678]]", event.toString() );
  }

  @Test
  public void testResourceAvailabilitySimpleEventCreation() {
    final ResourceAvailabilityEvent event =
        new ResourceAvailabilityEvent(
            ResourceType.Address,
            new Availability( 4, 2 ) );

    assertEquals( "type", ResourceType.Address, event.getType() );
    assertEquals( "availability size", 1, event.getAvailability().size() );
    assertEquals( "total", 4, Iterables.get( event.getAvailability(), 0 ).getTotal() );
    assertEquals( "available", 2, Iterables.get( event.getAvailability(), 0 ).getAvailable() );
  }

  @Test
  public void testResourceAvailabilityEventCreation() {
    final ResourceAvailabilityEvent event =
        new ResourceAvailabilityEvent(
            ResourceType.Instance,
            Arrays.<Availability>asList(
              new Availability( 4, 2, Arrays.<Tag>asList( new Dimension( "type", "c1.small" ) ) ),
              new Availability( 5, 3, Arrays.<Tag>asList( new Dimension( "type", "a1.sauce" ) ) ),
              new Availability( 6, 4, Arrays.<Tag>asList( new Dimension( "type", "f2.foo" ), new Type( "color", "red" ) ) )
            )
    );

    assertEquals( "type", ResourceType.Instance, event.getType() );
    assertEquals( "availability size", 3, event.getAvailability().size() );
    assertEquals( "a0 total", 4, Iterables.get(event.getAvailability(), 0).getTotal() );
    assertEquals( "a0 available", 2, Iterables.get(event.getAvailability(), 0).getAvailable() );
    assertEquals( "a0 tags size", 1, Iterables.get(event.getAvailability(), 0).getTags().size() );
    assertEquals( "a1 total", 5, Iterables.get(event.getAvailability(), 1).getTotal() );
    assertEquals( "a1 available", 3, Iterables.get(event.getAvailability(), 1).getAvailable() );
    assertEquals( "a1 tags size", 1, Iterables.get(event.getAvailability(), 1).getTags().size() );
    assertEquals( "a2 total", 6, Iterables.get(event.getAvailability(), 2).getTotal() );
    assertEquals( "a2 available", 4, Iterables.get(event.getAvailability(), 2).getAvailable() );
    assertEquals( "a2 tags size", 2, Iterables.get(event.getAvailability(), 2).getTags().size() );
    assertEquals( "event string", "[type:Instance,availability:[total:4,available:2,tags:[tag:type=c1.small]],[total:5,available:3,tags:[tag:type=a1.sauce]],[total:6,available:4,tags:[tag:type=f2.foo],[tag:color=red]]]", event.toString() );
  }

  private String uuid( final String seed ) {
    return UUID.nameUUIDFromBytes( seed.getBytes(Charsets.UTF_8) ).toString();
  }
}
