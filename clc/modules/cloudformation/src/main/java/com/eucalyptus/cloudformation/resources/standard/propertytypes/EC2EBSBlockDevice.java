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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;

public class EC2EBSBlockDevice {

  @Property
  private Boolean deleteOnTermination;

  @Property
  private Integer iops;

  @Property
  private String snapshotId;

  @Property
  private String volumeSize;

  @Property
  private String volumeType;

  public Boolean getDeleteOnTermination( ) {
    return deleteOnTermination;
  }

  public void setDeleteOnTermination( Boolean deleteOnTermination ) {
    this.deleteOnTermination = deleteOnTermination;
  }

  public Integer getIops( ) {
    return iops;
  }

  public void setIops( Integer iops ) {
    this.iops = iops;
  }

  public String getSnapshotId( ) {
    return snapshotId;
  }

  public void setSnapshotId( String snapshotId ) {
    this.snapshotId = snapshotId;
  }

  public String getVolumeSize( ) {
    return volumeSize;
  }

  public void setVolumeSize( String volumeSize ) {
    this.volumeSize = volumeSize;
  }

  public String getVolumeType( ) {
    return volumeType;
  }

  public void setVolumeType( String volumeType ) {
    this.volumeType = volumeType;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final EC2EBSBlockDevice that = (EC2EBSBlockDevice) o;
    return Objects.equals( getDeleteOnTermination( ), that.getDeleteOnTermination( ) ) &&
        Objects.equals( getIops( ), that.getIops( ) ) &&
        Objects.equals( getSnapshotId( ), that.getSnapshotId( ) ) &&
        Objects.equals( getVolumeSize( ), that.getVolumeSize( ) ) &&
        Objects.equals( getVolumeType( ), that.getVolumeType( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getDeleteOnTermination( ), getIops( ), getSnapshotId( ), getVolumeSize( ), getVolumeType( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "deleteOnTermination", deleteOnTermination )
        .add( "iops", iops )
        .add( "snapshotId", snapshotId )
        .add( "volumeSize", volumeSize )
        .add( "volumeType", volumeType )
        .toString( );
  }
}
