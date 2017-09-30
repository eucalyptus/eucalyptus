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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ScheduledInstancesEbs extends EucalyptusData {

  private Boolean deleteOnTermination;
  private Boolean encrypted;
  private Integer iops;
  private String snapshotId;
  private Integer volumeSize;

  public Boolean getDeleteOnTermination( ) {
    return deleteOnTermination;
  }

  public void setDeleteOnTermination( Boolean deleteOnTermination ) {
    this.deleteOnTermination = deleteOnTermination;
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

  public String getSnapshotId( ) {
    return snapshotId;
  }

  public void setSnapshotId( String snapshotId ) {
    this.snapshotId = snapshotId;
  }

  public Integer getVolumeSize( ) {
    return volumeSize;
  }

  public void setVolumeSize( Integer volumeSize ) {
    this.volumeSize = volumeSize;
  }
}
