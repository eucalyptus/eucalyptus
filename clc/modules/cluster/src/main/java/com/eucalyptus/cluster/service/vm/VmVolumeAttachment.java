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
 ************************************************************************/package com.eucalyptus.cluster.service.vm;

import java.util.Date;
import java.util.Objects;
import com.eucalyptus.crypto.util.Timestamps;
import com.google.common.base.MoreObjects;

/**
 *
 */
public final class VmVolumeAttachment {
  private final long attachmentTimestamp;
  private final String volumeId;
  private final String device;
  private final String remoteDevice;
  private final String state;

  public VmVolumeAttachment(
      final long attachmentTimestamp,
      final String volumeId,
      final String device,
      final String remoteDevice,
      final String state
  ) {
    this.attachmentTimestamp = attachmentTimestamp;
    this.volumeId = volumeId;
    this.device = device;
    this.remoteDevice = remoteDevice;
    this.state = state;
  }

  public long getAttachmentTimestamp() {
    return attachmentTimestamp;
  }

  public String getDevice() {
    return device;
  }

  public String getRemoteDevice() {
    return remoteDevice;
  }

  public String getState() {
    return state;
  }

  public String getVolumeId() {
    return volumeId;
  }

  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "volume-id", getVolumeId( ) )
        .add( "device", getDevice( ) )
        .add( "remote-device", getRemoteDevice( ) )
        .add( "state", getState( ) )
        .add( "attachment-timestamp", Timestamps.formatIso8601Timestamp( new Date( getAttachmentTimestamp( ) ) ) )
        .omitNullValues( )
        .toString( );
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final VmVolumeAttachment that = (VmVolumeAttachment) o;
    return Objects.equals( volumeId, that.volumeId ) &&
        Objects.equals( device, that.device );
  }

  @Override
  public int hashCode() {
    return Objects.hash( volumeId, device );
  }
}
