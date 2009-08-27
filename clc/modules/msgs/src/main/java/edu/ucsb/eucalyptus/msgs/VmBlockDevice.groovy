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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package edu.ucsb.eucalyptus.msgs

import edu.ucsb.eucalyptus.annotation.HttpParameterMapping;

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

public class AttachedVolume extends EucalyptusData {

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

  boolean equals(final Object o) {
    if ( this.is(o) ) return true;
    if ( !o || getClass() != o.class ) return false;
    AttachedVolume that = (AttachedVolume) o;
    if ( volumeId ? !volumeId.equals(that.volumeId) : that.volumeId != null ) return false;
    return true;
  }

  int hashCode() {
    return (volumeId ? volumeId.hashCode() : 0);
  }

}

public class Snapshot extends EucalyptusData {

  String snapshotId;
  String volumeId;
  String status;
  Date startTime = new Date();
  String progress;
}
