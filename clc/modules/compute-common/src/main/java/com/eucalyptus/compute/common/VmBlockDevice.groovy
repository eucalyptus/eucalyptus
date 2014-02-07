/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

@GroovyAddClassUUID
package com.eucalyptus.compute.common;

import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.binding.HttpEmbedded
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

public class BlockVolumeMessage extends ComputeMessage {
  
  public BlockVolumeMessage( ) {
    super( );
  }
  
  public BlockVolumeMessage( ComputeMessage msg ) {
    super( msg );
  }
  
  public BlockVolumeMessage( String userId ) {
    super( userId );
  }
}
public class BlockSnapshotMessage extends ComputeMessage {
  
  public BlockSnapshotMessage( ) {
    super( );
  }
  
  public BlockSnapshotMessage( ComputeMessage msg ) {
    super( msg );
  }
  
  public BlockSnapshotMessage( String userId ) {
    super( userId );
  }
}


public class CreateVolumeType extends BlockVolumeMessage {
  String size;
  String snapshotId;
  String availabilityZone;
  String volumeType = "standard"
  Integer iops
}
public class CreateVolumeResponseType extends BlockVolumeMessage {
  
  Volume volume = new Volume();
}

public class DeleteVolumeType extends BlockVolumeMessage {
  
  String volumeId;
}
public class DeleteVolumeResponseType extends BlockVolumeMessage {
}

public class DescribeVolumesType extends BlockVolumeMessage {
  
  @HttpParameterMapping (parameter = "VolumeId")
  ArrayList<String> volumeSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}
public class DescribeVolumesResponseType extends BlockVolumeMessage {
  
  ArrayList<Volume> volumeSet = new ArrayList<Volume>();
}

public class AttachVolumeType extends BlockVolumeMessage {
  
  String volumeId;
  String instanceId;
  String device;
  String remoteDevice;
  public AttachVolumeType( ) {
    super( );
  }
  public AttachVolumeType( String volumeId, String instanceId, String device, String remoteDevice ) {
    super( );
    this.volumeId = volumeId;
    this.instanceId = instanceId;
    this.device = device;
    this.remoteDevice = remoteDevice;
  }
  
}
public class AttachVolumeResponseType extends BlockVolumeMessage {
  
  AttachedVolume attachedVolume = new AttachedVolume();
}

public class DetachVolumeType extends BlockVolumeMessage {
  
  String volumeId;
  String instanceId;
  String device;
  String remoteDevice;
  Boolean force = false;
}
public class DetachVolumeResponseType extends BlockVolumeMessage {
  
  AttachedVolume detachedVolume = new AttachedVolume();
}

public class CreateSnapshotType extends BlockSnapshotMessage {
  
  String volumeId;
  String description;
}
public class CreateSnapshotResponseType extends BlockSnapshotMessage {
  
  Snapshot snapshot = new Snapshot();
}

public class DeleteSnapshotType extends BlockSnapshotMessage {
  
  String snapshotId;
}
public class DeleteSnapshotResponseType extends BlockSnapshotMessage {
}

public class DescribeSnapshotsType extends BlockSnapshotMessage {
  
  @HttpParameterMapping (parameter = "SnapshotId")
  ArrayList<String> snapshotSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Owner")
  ArrayList<String> ownersSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "RestorableBy")
  ArrayList<String> restorableBySet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}
public class DescribeSnapshotsResponseType extends BlockSnapshotMessage {
  
  ArrayList<Snapshot> snapshotSet = new ArrayList<Snapshot>();
}

public class Volume extends EucalyptusData {
  
  String volumeId;
  String size;
  String snapshotId;
  String availabilityZone;
  String status;
  Date createTime = new Date();
  ArrayList<AttachedVolume> attachmentSet = new ArrayList<AttachedVolume>();
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>();
  
  public Volume() {
  }
  public Volume(String volumeId) {
    this.volumeId = volumeId;
  }
}

public class AttachedVolume extends EucalyptusData implements Comparable<AttachedVolume> {
  
  String volumeId;
  String instanceId;
  String device;
  String remoteDevice;
  String status;
  Date attachTime = new Date();
  
  def AttachedVolume(final String volumeId, final String instanceId, final String device, final String remoteDevice) {
    this.volumeId = volumeId;
    this.instanceId = instanceId;
    this.device = device;
    this.remoteDevice = remoteDevice;
    this.status = "attaching";
  }
  
  public AttachedVolume( String volumeId ) {
    this.volumeId = volumeId;
  }
  
  public AttachedVolume() {
  }
  
  public boolean equals(final Object o) {
    if ( this.is(o) ) return true;
    if ( o == null || !getClass().equals( o.class ) ) return false;
    AttachedVolume that = (AttachedVolume) o;
    if ( volumeId ? !volumeId.equals(that.volumeId) : that.volumeId != null ) return false;
    return true;
  }
  
  public int hashCode() {
    return (volumeId ? volumeId.hashCode() : 0);
  }
  
  public int compareTo( AttachedVolume that ) {
    return this.volumeId.compareTo( that.volumeId );
  }
  
  public String toString() {
    return "AttachedVolume ${volumeId} ${instanceId} ${status} ${device} ${remoteDevice} ${attachTime}"
  }
}

public class Snapshot extends EucalyptusData {
  
  String snapshotId;
  String volumeId;
  String status;
  Date startTime = new Date();
  String progress;
  String ownerId;
  String volumeSize = "n/a";
  String description;
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>();
}
//TODO:ADDED
public class ModifySnapshotAttributeType extends BlockSnapshotMessage {
  String snapshotId;
  ArrayList<VolumePermissionType> removeVolumePermission = new ArrayList<VolumePermissionType>();
  ArrayList<VolumePermissionType> addVolumePermission = new ArrayList<VolumePermissionType>();
  public ModifySnapshotAttributeType() {  }
}
public class VolumePermissionType extends EucalyptusData {
  String userId;
  String group;
  public CreateVolumePermissionItemType() {  }
}
public class ModifySnapshotAttributeResponseType extends BlockSnapshotMessage {
  public ModifySnapshotAttributeResponseType() {  }
}
public class DescribeSnapshotAttributeResponseType extends BlockSnapshotMessage {
  String snapshotId;
  ArrayList<VolumePermissionType> createVolumePermission = new ArrayList<VolumePermissionType>();
  public DescribeSnapshotAttributeResponseType() {  }
}
public class DescribeSnapshotAttributeType extends BlockSnapshotMessage {
  String snapshotId;
  public DescribeSnapshotAttributeType() {  }
}
public class ResetSnapshotAttributeResponseType extends BlockSnapshotMessage {
  public ResetSnapshotAttributeResponseType() {  }
}
public class ResetSnapshotAttributeType extends BlockSnapshotMessage {
  String snapshotId;
  public ResetSnapshotAttributeType() {  }
}
