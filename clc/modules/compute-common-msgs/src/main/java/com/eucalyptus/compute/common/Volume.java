/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
