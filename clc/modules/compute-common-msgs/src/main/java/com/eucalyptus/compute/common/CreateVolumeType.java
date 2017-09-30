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
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.binding.HttpEmbedded;
import edu.ucsb.eucalyptus.msgs.HasTags;

public class CreateVolumeType extends BlockVolumeMessage implements HasTags {

  private String size;
  private String snapshotId;
  private String availabilityZone;
  private String volumeType = "standard";
  private Integer iops;
  private Boolean encrypted;
  private String kmsKeyId;
  @HttpEmbedded( multiple = true )
  private ArrayList<ResourceTagSpecification> tagSpecification = new ArrayList<ResourceTagSpecification>( );

  @Override
  public Set<String> getTagKeys( @Nullable String resourceType, @Nullable String resourceId ) {
    return getTagKeys( tagSpecification, resourceType, resourceId );
  }

  @Override
  public String getTagValue( @Nullable String resourceType, @Nullable String resourceId, @Nonnull final String tagKey ) {
    return getTagValue( tagSpecification, resourceType, resourceId, tagKey );
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

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public String getVolumeType( ) {
    return volumeType;
  }

  public void setVolumeType( String volumeType ) {
    this.volumeType = volumeType;
  }

  public Integer getIops( ) {
    return iops;
  }

  public void setIops( Integer iops ) {
    this.iops = iops;
  }

  public Boolean getEncrypted( ) {
    return encrypted;
  }

  public void setEncrypted( Boolean encrypted ) {
    this.encrypted = encrypted;
  }

  public String getKmsKeyId( ) {
    return kmsKeyId;
  }

  public void setKmsKeyId( String kmsKeyId ) {
    this.kmsKeyId = kmsKeyId;
  }

  public ArrayList<ResourceTagSpecification> getTagSpecification( ) {
    return tagSpecification;
  }

  public void setTagSpecification( ArrayList<ResourceTagSpecification> tagSpecification ) {
    this.tagSpecification = tagSpecification;
  }
}
