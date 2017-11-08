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

import java.util.ArrayList;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class AWSEC2VolumeProperties implements ResourceProperties {

  @Property
  private Boolean autoEnableIO;

  @Required
  @Property
  private String availabilityZone;

  @Property
  private Boolean encrypted;

  @Property
  private Integer iops;

  @Property
  private String kmsKeyId;

  @Property
  private String size;

  @Property
  private String snapshotId;

  @Property
  private ArrayList<EC2Tag> tags = Lists.newArrayList( );

  @Property
  private String volumeType;

  public Boolean getAutoEnableIO( ) {
    return autoEnableIO;
  }

  public void setAutoEnableIO( Boolean autoEnableIO ) {
    this.autoEnableIO = autoEnableIO;
  }

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public Boolean getEncrypted( ) {
    return encrypted;
  }

  public void setEncrypted( Boolean encrypted ) {
    this.encrypted = encrypted;
  }

  public Integer getIops( ) {
    return iops;
  }

  public void setIops( Integer iops ) {
    this.iops = iops;
  }

  public String getKmsKeyId( ) {
    return kmsKeyId;
  }

  public void setKmsKeyId( String kmsKeyId ) {
    this.kmsKeyId = kmsKeyId;
  }

  public String getSize( ) {
    return size;
  }

  public void setSize( String size ) {
    this.size = size;
  }

  public String getSnapshotId( ) {
    return snapshotId;
  }

  public void setSnapshotId( String snapshotId ) {
    this.snapshotId = snapshotId;
  }

  public ArrayList<EC2Tag> getTags( ) {
    return tags;
  }

  public void setTags( ArrayList<EC2Tag> tags ) {
    this.tags = tags;
  }

  public String getVolumeType( ) {
    return volumeType;
  }

  public void setVolumeType( String volumeType ) {
    this.volumeType = volumeType;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "autoEnableIO", autoEnableIO )
        .add( "availabilityZone", availabilityZone )
        .add( "encrypted", encrypted )
        .add( "iops", iops )
        .add( "kmsKeyId", kmsKeyId )
        .add( "size", size )
        .add( "snapshotId", snapshotId )
        .add( "tags", tags )
        .add( "volumeType", volumeType )
        .toString( );
  }
}
