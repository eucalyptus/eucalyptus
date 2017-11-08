/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.autoscaling.common.internal.configurations;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 *
 */
@Embeddable
public class BlockDeviceMapping {
  
  @Column( name = "metadata_device_name", nullable = false )
  private String deviceName;

  @Column( name = "metadata_virtual_name" )
  private String virtualName;

  // EBS
  @Column( name = "metadata_snapshot_id" )
  private String snapshotId;

  // EBS
  @Column( name = "metadata_volume_size" )
  private Integer volumeSize;

  protected BlockDeviceMapping() {    
  }

  protected BlockDeviceMapping( final String deviceName ) {
    setDeviceName( deviceName );
  }

  protected BlockDeviceMapping( final String deviceName,
                                final String virtualName,
                                final String snapshotId,
                                final Integer volumeSize ) {
    setDeviceName( deviceName );
    setVirtualName( virtualName );
    setSnapshotId( snapshotId );
    setVolumeSize( volumeSize );
  }

  public String getDeviceName() {
    return deviceName;
  }

  public void setDeviceName( final String deviceName ) {
    this.deviceName = deviceName;
  }

  @Nullable
  public String getVirtualName() {
    return virtualName;
  }

  public void setVirtualName( final String virtualName ) {
    this.virtualName = virtualName;
  }

  @Nullable
  public String getSnapshotId() {
    return snapshotId;
  }

  public void setSnapshotId( final String snapshotId ) {
    this.snapshotId = snapshotId;
  }

  @Nullable
  public Integer getVolumeSize() {
    return volumeSize;
  }

  public void setVolumeSize( final Integer volumeSize ) {
    this.volumeSize = volumeSize;
  }

  @SuppressWarnings( "RedundantIfStatement" )
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    final BlockDeviceMapping that = (BlockDeviceMapping) o;

    if ( !deviceName.equals( that.deviceName ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return deviceName.hashCode();
  }
}
