/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.msgs

import com.eucalyptus.binding.HttpParameterMapping;

public class BlockVolumeMessage extends EucalyptusMessage {}
public class BlockSnapshotMessage extends EucalyptusMessage {}

public class CreateVolumeType extends BlockVolumeMessage {

  String size;
  String snapshotId;
  String availabilityZone;
}
public class CreateVolumeResponseType extends BlockVolumeMessage {

  Volume volume = new Volume();
}
public class DeleteVolumeType extends BlockVolumeMessage {

  String volumeId;
}
public class DeleteVolumeResponseType extends BlockVolumeMessage {

  boolean _return;
}
public class DescribeVolumesType extends BlockVolumeMessage {

  @HttpParameterMapping (parameter = "VolumeId")
  ArrayList<String> volumeSet = new ArrayList<String>();
}
public class DescribeVolumesResponseType extends BlockVolumeMessage {

  ArrayList<Volume> volumeSet = new ArrayList<Volume>();
}
public class AttachVolumeType extends BlockVolumeMessage {

  String volumeId;
  String instanceId;
  String device;
  String remoteDevice;
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

  boolean _return;
}
public class DescribeSnapshotsType extends BlockSnapshotMessage {

  @HttpParameterMapping (parameter = "SnapshotId")
  ArrayList<String> snapshotSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Owner")
  ArrayList<String> ownersSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "RestorableBy")
  ArrayList<String> restorableBySet = new ArrayList<String>();
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

  public Volume() {}
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

  public AttachedVolume() {}

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
  String volumeSize = "-1";
  String description;
}
