/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cluster.common.msgs;

import java.util.Date;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmStandardVolumeAttachment;
import com.google.common.base.Function;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class AttachedVolume extends EucalyptusData implements Comparable<AttachedVolume> {

  private String volumeId;
  private String instanceId;
  private String device;
  private String remoteDevice;
  private String status;
  private Date attachTime = new Date( );

  public AttachedVolume( final String volumeId, final String instanceId, final String device, final String remoteDevice ) {
    this.volumeId = volumeId;
    this.instanceId = instanceId;
    this.device = device;
    this.remoteDevice = remoteDevice;
    this.status = "attaching";
  }

  public AttachedVolume( String volumeId ) {
    this.volumeId = volumeId;
  }

  public AttachedVolume( ) {
  }

  public static Function<AttachedVolume, VmStandardVolumeAttachment> toStandardVolumeAttachment( final VmInstance vm ) {
    return new Function<AttachedVolume, VmStandardVolumeAttachment>( ) {
      @Override
      public VmStandardVolumeAttachment apply( AttachedVolume vol ) {
        return new VmStandardVolumeAttachment( vm, vol.getVolumeId( ), vol.getDevice( ), vol.getRemoteDevice( ), vol.getStatus( ), vol.getAttachTime( ), false, Boolean.FALSE );
      }

    };
  }

  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || !getClass( ).equals( o.getClass( ) ) ) return false;
    AttachedVolume that = (AttachedVolume) o;
    return volumeId != null ? volumeId.equals( that.volumeId ) : that.volumeId == null;
  }

  public int hashCode( ) {
    return volumeId != null ? volumeId.hashCode( ) : 0;
  }

  public int compareTo( AttachedVolume that ) {
    return this.volumeId.compareTo( that.volumeId );
  }

  public String toString( ) {
    return "AttachedVolume " + volumeId + " " + instanceId + " " + status + " " + device + " " + remoteDevice + " " + String.valueOf( attachTime );
  }

  public String getVolumeId( ) {
    return volumeId;
  }

  public void setVolumeId( String volumeId ) {
    this.volumeId = volumeId;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getDevice( ) {
    return device;
  }

  public void setDevice( String device ) {
    this.device = device;
  }

  public String getRemoteDevice( ) {
    return remoteDevice;
  }

  public void setRemoteDevice( String remoteDevice ) {
    this.remoteDevice = remoteDevice;
  }

  public String getStatus( ) {
    return status;
  }

  public void setStatus( String status ) {
    this.status = status;
  }

  public Date getAttachTime( ) {
    return attachTime;
  }

  public void setAttachTime( Date attachTime ) {
    this.attachTime = attachTime;
  }
}
