/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.autoscaling.configurations;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 *
 */
@Embeddable
public class EbsParameters {
  
  //TODO:STEVE: Fix persistence annotations so column names are used ...
  @Column( name = "metadata_snapshot_id" )
  private String snapshotId;

  @Column( name = "metadata_volume_size" )
  private Integer volumeSize;

  protected EbsParameters() {      
  }

  protected EbsParameters( final String snapshotId,
                           final Integer volumeSize ) {
    this.snapshotId = snapshotId;
    this.volumeSize = volumeSize;
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
}
