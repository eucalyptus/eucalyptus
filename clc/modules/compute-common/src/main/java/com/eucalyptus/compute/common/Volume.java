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
import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class Volume extends EucalyptusData {

  private String volumeId;
  private String size;
  private String volumeType = "standard";
  private Integer iops;
  private Boolean encrypted;
  private String snapshotId;
  private String availabilityZone;
  private String status;
  private Date createTime = new Date( );
  private ArrayList<AttachedVolume> attachmentSet = new ArrayList<AttachedVolume>( );
  private ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>( );

  public Volume( ) {
  }

  public Volume( String volumeId ) {
    this.volumeId = volumeId;
  }

  public String getVolumeId( ) {
    return volumeId;
  }

  public void setVolumeId( String volumeId ) {
    this.volumeId = volumeId;
  }

  public String getSize( ) {
    return size;
  }

  public void setSize( String size ) {
    this.size = size;
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

  public String getStatus( ) {
    return status;
  }

  public void setStatus( String status ) {
    this.status = status;
  }

  public Date getCreateTime( ) {
    return createTime;
  }

  public void setCreateTime( Date createTime ) {
    this.createTime = createTime;
  }

  public ArrayList<AttachedVolume> getAttachmentSet( ) {
    return attachmentSet;
  }

  public void setAttachmentSet( ArrayList<AttachedVolume> attachmentSet ) {
    this.attachmentSet = attachmentSet;
  }

  public ArrayList<ResourceTag> getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ArrayList<ResourceTag> tagSet ) {
    this.tagSet = tagSet;
  }
}
