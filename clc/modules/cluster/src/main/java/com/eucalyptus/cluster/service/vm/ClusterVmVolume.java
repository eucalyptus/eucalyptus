/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
 ************************************************************************/package com.eucalyptus.cluster.service.vm;

import java.util.Date;
import java.util.Objects;
import com.eucalyptus.cluster.common.msgs.VolumeType;
import com.eucalyptus.crypto.util.Timestamps;
import com.google.common.base.MoreObjects;

/**
 *
 */
public final class ClusterVmVolume {
  private final long attachmentTimestamp;
  private final String volumeId;
  private final String device;
  private final String remoteDevice;
  private final String state;

  private ClusterVmVolume(
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

  public static ClusterVmVolume of(
      final long attachmentTimestamp,
      final String volumeId,
      final String device,
      final String remoteDevice,
      final String state
  ) {
    return new ClusterVmVolume(
        attachmentTimestamp,
        volumeId,
        device,
        remoteDevice,
        state
    );
  }

  public static ClusterVmVolume fromNodeVolume(
      final long attachmentTimestamp,
      final VolumeType volume
  ) {
    return of(
        attachmentTimestamp,
        volume.getVolumeId( ),
        volume.getLocalDev( ),
        volume.getRemoteDev( ),
        volume.getState( )

    );
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
    final ClusterVmVolume that = (ClusterVmVolume) o;
    return Objects.equals( getVolumeId( ), that.getVolumeId( ) ) &&
        Objects.equals( getDevice( ), that.getDevice( ) ) &&
        Objects.equals( getRemoteDevice( ), that.getRemoteDevice( ) ) &&
        Objects.equals( getState( ), that.getState( ) );
  }

  @Override
  public int hashCode() {
    return Objects.hash( getVolumeId( ), getDevice( ), getRemoteDevice( ), getState( ) );
  }
}
