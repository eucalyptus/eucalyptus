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

public class Snapshot extends EucalyptusData {

  private String snapshotId;
  private String volumeId;
  private String status;
  private Date startTime = new Date( );
  private String progress;
  private String ownerId;
  private String ownerAlias;
  private String volumeSize = "n/a";
  private Boolean encrypted;
  private String description;
  private ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>( );

  public String getSnapshotId( ) {
    return snapshotId;
  }

  public void setSnapshotId( String snapshotId ) {
    this.snapshotId = snapshotId;
  }

  public String getVolumeId( ) {
    return volumeId;
  }

  public void setVolumeId( String volumeId ) {
    this.volumeId = volumeId;
  }

  public String getStatus( ) {
    return status;
  }

  public void setStatus( String status ) {
    this.status = status;
  }

  public Date getStartTime( ) {
    return startTime;
  }

  public void setStartTime( Date startTime ) {
    this.startTime = startTime;
  }

  public String getProgress( ) {
    return progress;
  }

  public void setProgress( String progress ) {
    this.progress = progress;
  }

  public String getOwnerId( ) {
    return ownerId;
  }

  public void setOwnerId( String ownerId ) {
    this.ownerId = ownerId;
  }

  public String getOwnerAlias( ) {
    return ownerAlias;
  }

  public void setOwnerAlias( String ownerAlias ) {
    this.ownerAlias = ownerAlias;
  }

  public String getVolumeSize( ) {
    return volumeSize;
  }

  public void setVolumeSize( String volumeSize ) {
    this.volumeSize = volumeSize;
  }

  public Boolean getEncrypted( ) {
    return encrypted;
  }

  public void setEncrypted( Boolean encrypted ) {
    this.encrypted = encrypted;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public ArrayList<ResourceTag> getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ArrayList<ResourceTag> tagSet ) {
    this.tagSet = tagSet;
  }
}
