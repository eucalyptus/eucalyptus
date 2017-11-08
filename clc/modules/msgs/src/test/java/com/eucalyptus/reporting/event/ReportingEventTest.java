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
package com.eucalyptus.reporting.event;

import java.util.Arrays;
import java.util.UUID;
import static org.junit.Assert.*;
import static com.eucalyptus.reporting.event.AddressEvent.AddressAction;
import static com.eucalyptus.reporting.event.EventActionInfo.InstanceEventActionInfo;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Availability;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Dimension;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Tag;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Type;
import org.junit.Test;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;

/**
 * Unit tests for event classes
 */
public class ReportingEventTest {

  @Test
  public void testAddressAllocateEventCreation() {
    final AddressEvent event = AddressEvent.with(
        "127.0.0.1",
        userFullName( "testaccount" ),
        "testaccount",
        AddressEvent.forAllocate()
        );

    assertEquals( "address", "127.0.0.1", event.getAddress() );
    assertEquals( "account number", "100000000000", event.getAccountId() );
    assertEquals( "account name", "testaccount", event.getAccountName() );
    assertEquals( "user number", "testaccount", event.getUserId() );
    assertEquals( "user name", "testaccount", event.getUserName() );
    assertEquals( "action info type", EventActionInfo.class, event.getActionInfo().getClass() );
    assertEquals( "action info action", AddressAction.ALLOCATE, event.getActionInfo().getAction() );
    assertEquals( "event string", "[address:127.0.0.1,userId:testaccount,accountId:100000000000,actionInfo:[action:ALLOCATE]]", event.toString() );
  }

  @Test
  public void testAddressReleaseEventCreation() {
    final AddressEvent event = AddressEvent.with(
        "127.0.0.1",
        userFullName( "testaccount" ),
        "testaccount",
        AddressEvent.forRelease()
    );

    assertEquals( "address", "127.0.0.1", event.getAddress() );
    assertEquals( "account number", "100000000000", event.getAccountId() );
    assertEquals( "account name", "testaccount", event.getAccountName() );
    assertEquals( "user number", "testaccount", event.getUserId() );
    assertEquals( "user name", "testaccount", event.getUserName() );
    assertEquals( "action info type", EventActionInfo.class, event.getActionInfo().getClass() );
    assertEquals( "action info action", AddressAction.RELEASE, event.getActionInfo().getAction() );
    assertEquals( "event string", "[address:127.0.0.1,userId:testaccount,accountId:100000000000,actionInfo:[action:RELEASE]]", event.toString() );
  }

  @Test
  public void testAddressAssociateEventCreation() {
    final AddressEvent event = AddressEvent.with(
        "127.0.0.1",
        userFullName( "testaccount" ),
        "testaccount",
        AddressEvent.forAssociate( uuid( "instance" ), "i-12345678" )
    );

    assertEquals( "address", "127.0.0.1", event.getAddress() );
    assertEquals( "account number", "100000000000", event.getAccountId() );
    assertEquals( "account name", "testaccount", event.getAccountName() );
    assertEquals( "user number", "testaccount", event.getUserId() );
    assertEquals( "user name", "testaccount", event.getUserName() );
    assertEquals( "action info type", InstanceEventActionInfo.class, event.getActionInfo().getClass() );
    assertEquals( "action info action", AddressAction.ASSOCIATE, event.getActionInfo().getAction() );
    assertEquals( "action info instance uuid", uuid("instance"), ((InstanceEventActionInfo)event.getActionInfo()).getInstanceUuid() );
    assertEquals( "action info instance id ", "i-12345678", ((InstanceEventActionInfo)event.getActionInfo()).getInstanceId() );
    assertEquals( "event string", "[address:127.0.0.1,userId:testaccount,accountId:100000000000,actionInfo:[action:ASSOCIATE,instanceUuid:7123a699-d77d-3647-9a1d-8ece2c4f1c16,instanceId:i-12345678]]", event.toString() );
  }

  @Test
  public void testAddressDisassociateEventCreation() {
    final AddressEvent event = AddressEvent.with(
        "127.0.0.1",
        userFullName( "testaccount" ),
        "testaccount",
        AddressEvent.forDisassociate(uuid("instance2"), "i-12345678")
    );

    assertEquals( "address", "127.0.0.1", event.getAddress() );
    assertEquals( "account number", "100000000000", event.getAccountId() );
    assertEquals( "account name", "testaccount", event.getAccountName() );
    assertEquals( "user number", "testaccount", event.getUserId() );
    assertEquals( "user name", "testaccount", event.getUserName() );
    assertEquals( "action info type", InstanceEventActionInfo.class, event.getActionInfo().getClass() );
    assertEquals( "action info action", AddressAction.DISASSOCIATE, event.getActionInfo().getAction() );
    assertEquals( "action info instance uuid", uuid("instance2"), ((InstanceEventActionInfo)event.getActionInfo()).getInstanceUuid() );
    assertEquals( "action info instance id", "i-12345678", ((InstanceEventActionInfo)event.getActionInfo()).getInstanceId() );
    assertEquals( "event string", "[address:127.0.0.1,userId:testaccount,accountId:100000000000,actionInfo:[action:DISASSOCIATE,instanceUuid:781e6aca-85f8-3f45-9e1f-58d2fb126bca,instanceId:i-12345678]]", event.toString() );
  }

  @Test
  public void testInstanceEventCreation() {
    final long timestamp = 1347987263899L;
    final long valueTimestamp = timestamp - 2000L;
    final InstanceUsageEvent event = new InstanceUsageEvent(
            uuid( "i-00000001" ),
            "i-00000001",
            "metric",
            1L,
            "dimension",
            12.17,
            valueTimestamp
        );

    assertEquals( "uuid", uuid( "i-00000001" ), event.getUuid() );
    assertEquals( "id", "i-00000001", event.getInstanceId() );
    assertEquals( "metric", "metric", event.getMetric() );
    assertEquals( "sequence", (Long)1L, event.getSequenceNum() );
    assertEquals( "dimension", "dimension", event.getDimension() );
    assertEquals( "value", (Double)12.17, event.getValue() );
    assertEquals( "event string", "InstanceUsageEvent [uuid=51b56c1f-8c0d-3096-8c5e-e78ae6c8f4c0, instanceId=i-00000001, metric=metric, sequenceNum=1, dimension=dimension, value=12.17, valueTimestamp=1347987261899]", event.toString() );
  }

  @Test(expected=IllegalArgumentException.class)
  public void testInstanceEventCreationFailure() {
    new InstanceUsageEvent(
        uuid( "i-00000001" ),
        "i-00000001",
        null,
        null,
        null,
        null,
        System.currentTimeMillis()
    );
  }

  @Test
  public void testResourceAvailabilitySimpleEventCreation() {
    final ResourceAvailabilityEvent event =
        new ResourceAvailabilityEvent(
            ResourceType.Address,
            new Availability( 4, 2 ) );

    assertEquals( "type", ResourceType.Address, event.getType() );
    assertEquals( "availability size", 1, event.getAvailability().size() );
    assertEquals( "total", 4, Iterables.get(event.getAvailability(), 0).getTotal() );
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

  @Test
  public void testS3ObjectEventCreation() {
    final S3ObjectEvent putEvent = S3ObjectEvent.with(
        S3ObjectEvent.forS3ObjectCreate(),
        "bucket1",
        "object1",
        null,
        Principals.systemFullName().getUserId(),
        Principals.systemFullName().getUserName(),
        Principals.systemFullName().getAccountNumber(),
        12L
    );

    assertEquals("action", S3ObjectEvent.S3ObjectAction.OBJECTCREATE, putEvent.getAction());
    assertEquals("bucket name", "bucket1", putEvent.getBucketName());
    assertEquals("object key", "object1", putEvent.getObjectKey());
    assertNull("version", putEvent.getVersion());
    assertEquals("owner", Principals.systemFullName().getUserId(), putEvent.getUserId());
    assertEquals("size", (Long) 12L, putEvent.getSize());
    assertEquals("get event string", "S3ObjectEvent [action=OBJECTCREATE, userId=eucalyptus, size=12, bucketName=bucket1, objectKey=object1, version=null]", putEvent.toString());

    final S3ObjectEvent deleteEvent = S3ObjectEvent.with(
        S3ObjectEvent.forS3ObjectDelete(),
        "bucket1",
        "object1",
        "version1",
        Principals.systemFullName().getUserId(),
        Principals.systemFullName().getUserName(),
        Principals.systemFullName().getAccountNumber(),
        12L
    );

    assertEquals("action", S3ObjectEvent.S3ObjectAction.OBJECTDELETE, deleteEvent.getAction());
    assertEquals("bucket name", "bucket1", deleteEvent.getBucketName());
    assertEquals("version", "version1", deleteEvent.getVersion());
    assertEquals("owner", Principals.systemFullName().getUserId(), deleteEvent.getUserId());
    assertEquals("size", (Long) 12L, deleteEvent.getSize());
    assertEquals("get event string", "S3ObjectEvent [action=OBJECTDELETE, userId=eucalyptus, size=12, bucketName=bucket1, objectKey=object1, version=version1]", deleteEvent.toString());
  }

  @Test
  public void testVolumeCreateEvent() {
    final VolumeEvent event = VolumeEvent.with(
        VolumeEvent.forVolumeCreate(),
        uuid("vol-00000001"),
        "vol-00000001",
        123,
        Principals.systemFullName(),
        "PARTI001"
    );

    assertEquals( "uuid", uuid("vol-00000001"), event.getUuid() );
    assertEquals( "id", "vol-00000001", event.getVolumeId() );
    assertEquals( "size", 123, event.getSizeGB() );
    assertEquals( "userId", Principals.systemFullName().getUserId(), event.getUserId() );
    assertEquals( "userName", Principals.systemFullName().getUserName(), event.getUserName() );
    assertEquals( "accountNumber", Principals.systemFullName().getAccountNumber(), event.getAccountNumber() );
    assertEquals( "Availability zone", "PARTI001", event.getAvailabilityZone() );
    assertEquals( "action info type", EventActionInfo.class, event.getActionInfo().getClass() );
    assertEquals( "action info action", VolumeEvent.VolumeAction.VOLUMECREATE, event.getActionInfo().getAction() );
    assertEquals( "event string", "VolumeEvent [actionInfo=[action:VOLUMECREATE], uuid=ed5fb9b5-225a-387a-936c-0032cf2afdca, sizeGB=123, userId=eucalyptus, volumeId=vol-00000001, availabilityZone=PARTI001]", event.toString() );
  }

  @Test
  public void testVolumeDeleteEvent() {
    final VolumeEvent event = VolumeEvent.with(
        VolumeEvent.forVolumeDelete(),
        uuid("vol-00000001"),
        "vol-00000001",
        123,
        Principals.systemFullName(),
        "PARTI001"
    );

    assertEquals( "uuid", uuid("vol-00000001"), event.getUuid() );
    assertEquals( "id", "vol-00000001", event.getVolumeId() );
    assertEquals( "size", 123, event.getSizeGB() );
    assertEquals( "userId", Principals.systemFullName().getUserId(), event.getUserId() );
    assertEquals( "userName", Principals.systemFullName().getUserName(), event.getUserName() );
    assertEquals( "accountNumber", Principals.systemFullName().getAccountNumber(), event.getAccountNumber() );
    assertEquals( "Availability zone", "PARTI001", event.getAvailabilityZone() );
    assertEquals( "action info type", EventActionInfo.class, event.getActionInfo().getClass() );
    assertEquals( "action info action", VolumeEvent.VolumeAction.VOLUMEDELETE, event.getActionInfo().getAction() );
    assertEquals( "event string", "VolumeEvent [actionInfo=[action:VOLUMEDELETE], uuid=ed5fb9b5-225a-387a-936c-0032cf2afdca, sizeGB=123, userId=eucalyptus, volumeId=vol-00000001, availabilityZone=PARTI001]", event.toString() );
  }

  @Test
  public void testVolumeAttachEvent() {
    final VolumeEvent event = VolumeEvent.with(
        VolumeEvent.forVolumeAttach( uuid("i-00000001"), "i-00000001" ),
        uuid("vol-00000001"),
        "vol-00000001",
        123,
        Principals.systemFullName(),
        "PARTI001"
    );

    assertEquals( "uuid", uuid("vol-00000001"), event.getUuid() );
    assertEquals( "id", "vol-00000001", event.getVolumeId() );
    assertEquals( "size", 123, event.getSizeGB() );
    assertEquals( "userId", Principals.systemFullName().getUserId(), event.getUserId() );
    assertEquals( "userName", Principals.systemFullName().getUserName(), event.getUserName() );
    assertEquals( "accountNumber", Principals.systemFullName().getAccountNumber(), event.getAccountNumber() );
    assertEquals( "Availability zone", "PARTI001", event.getAvailabilityZone() );
    assertEquals( "action info type", InstanceEventActionInfo.class, event.getActionInfo().getClass() );
    assertEquals( "action info action", VolumeEvent.VolumeAction.VOLUMEATTACH, event.getActionInfo().getAction() );
    assertEquals( "action info instance uuid", uuid("i-00000001"), ((InstanceEventActionInfo)event.getActionInfo()).getInstanceUuid() );
    assertEquals( "action info instance id", "i-00000001", ((InstanceEventActionInfo)event.getActionInfo()).getInstanceId() );
    assertEquals( "event string", "VolumeEvent [actionInfo=[action:VOLUMEATTACH,instanceUuid:51b56c1f-8c0d-3096-8c5e-e78ae6c8f4c0,instanceId:i-00000001], uuid=ed5fb9b5-225a-387a-936c-0032cf2afdca, sizeGB=123, userId=eucalyptus, volumeId=vol-00000001, availabilityZone=PARTI001]", event.toString() );
  }

  @Test
  public void testVolumeDetachEvent() {
    final VolumeEvent event = VolumeEvent.with(
        VolumeEvent.forVolumeDetach( uuid("i-00000001"), "i-00000001" ),
        uuid("vol-00000001"),
        "vol-00000001",
        123,
        Principals.systemFullName(),
        "PARTI001"
    );

    assertEquals( "uuid", uuid("vol-00000001"), event.getUuid() );
    assertEquals( "id", "vol-00000001", event.getVolumeId() );
    assertEquals( "size", 123, event.getSizeGB() );
    assertEquals( "userId", Principals.systemFullName().getUserId(), event.getUserId() );
    assertEquals( "userName", Principals.systemFullName().getUserName(), event.getUserName() );
    assertEquals( "accountNumber", Principals.systemFullName().getAccountNumber(), event.getAccountNumber() );
    assertEquals( "Availability zone", "PARTI001", event.getAvailabilityZone() );
    assertEquals( "action info type", InstanceEventActionInfo.class, event.getActionInfo().getClass() );
    assertEquals( "action info action", VolumeEvent.VolumeAction.VOLUMEDETACH, event.getActionInfo().getAction() );
    assertEquals( "action info instance uuid", uuid("i-00000001"), ((InstanceEventActionInfo)event.getActionInfo()).getInstanceUuid() );
    assertEquals( "action info instance id", "i-00000001", ((InstanceEventActionInfo)event.getActionInfo()).getInstanceId() );
    assertEquals( "event string", "VolumeEvent [actionInfo=[action:VOLUMEDETACH,instanceUuid:51b56c1f-8c0d-3096-8c5e-e78ae6c8f4c0,instanceId:i-00000001], uuid=ed5fb9b5-225a-387a-936c-0032cf2afdca, sizeGB=123, userId=eucalyptus, volumeId=vol-00000001, availabilityZone=PARTI001]", event.toString() );
  }

  @Test
  public void testSnapshotCreateEvent() {
    final SnapShotEvent event = SnapShotEvent.with( SnapShotEvent.forSnapShotCreate(4, uuid("vol-00000001"), "vol-00000001" ), uuid("snap-00000001"), "snap-00000001", Principals.systemFullName().getUserId(), Principals.systemFullName().getUserName(), Principals.systemFullName().getAccountNumber() );

    assertEquals( "uuid", uuid("snap-00000001"), event.getUuid() );
    assertEquals( "id", "snap-00000001", event.getSnapshotId() );
    assertEquals( "owner", Principals.systemFullName().getUserId(), event.getUserId() );
    assertEquals( "action info type", SnapShotEvent.CreateActionInfo.class, event.getActionInfo().getClass() );
    assertEquals( "action info action", SnapShotEvent.SnapShotAction.SNAPSHOTCREATE, event.getActionInfo().getAction() );
    assertEquals( "action info size", Integer.valueOf(4), ((SnapShotEvent.CreateActionInfo)event.getActionInfo()).getSize() );
    assertEquals( "action info volume id", "vol-00000001", ((SnapShotEvent.CreateActionInfo)event.getActionInfo()).getVolumeId() );
    assertEquals( "action info volume uuid", uuid("vol-00000001"), ((SnapShotEvent.CreateActionInfo)event.getActionInfo()).getVolumeUuid() );
    assertEquals( "event string", "SnapShotEvent [actionInfo=[action:SNAPSHOTCREATE,size:4], userId=eucalyptus, snapshotId=snap-00000001, uuid=110a7f12-de63-3b1d-b5bc-ced21b270ae4]", event.toString() );
  }

  @Test
  public void testSnapshotDeleteEvent() {
    final SnapShotEvent event = SnapShotEvent.with( SnapShotEvent.forSnapShotDelete(), uuid("snap-00000001"), "snap-00000001", Principals.systemFullName().getUserId(), Principals.systemFullName().getUserName(), Principals.systemFullName().getAccountNumber() );

    assertEquals( "uuid", uuid("snap-00000001"), event.getUuid() );
    assertEquals( "id", "snap-00000001", event.getSnapshotId() );
    assertEquals( "owner", Principals.systemFullName().getUserId(), event.getUserId() );
    assertEquals( "action info type", EventActionInfo.class, event.getActionInfo().getClass() );
    assertEquals( "action info action", SnapShotEvent.SnapShotAction.SNAPSHOTDELETE, event.getActionInfo().getAction() );
    assertEquals( "event string", "SnapShotEvent [actionInfo=[action:SNAPSHOTDELETE], userId=eucalyptus, snapshotId=snap-00000001, uuid=110a7f12-de63-3b1d-b5bc-ced21b270ae4]", event.toString() );
  }

  private String uuid( final String seed ) {
    return UUID.nameUUIDFromBytes( seed.getBytes(Charsets.UTF_8) ).toString();
  }

  private OwnerFullName userFullName( final String name ) {
    return new OwnerFullName() {
      private static final long serialVersionUID = 1L;
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
