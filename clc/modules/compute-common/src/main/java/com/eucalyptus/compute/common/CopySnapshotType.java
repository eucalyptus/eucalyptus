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

public class CopySnapshotType extends BlockSnapshotMessage {

  private String description;
  private String destinationRegion;
  private String presignedUrl;
  private String sourceRegion;
  private String sourceSnapshotId;

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getDestinationRegion( ) {
    return destinationRegion;
  }

  public void setDestinationRegion( String destinationRegion ) {
    this.destinationRegion = destinationRegion;
  }

  public String getPresignedUrl( ) {
    return presignedUrl;
  }

  public void setPresignedUrl( String presignedUrl ) {
    this.presignedUrl = presignedUrl;
  }

  public String getSourceRegion( ) {
    return sourceRegion;
  }

  public void setSourceRegion( String sourceRegion ) {
    this.sourceRegion = sourceRegion;
  }

  public String getSourceSnapshotId( ) {
    return sourceSnapshotId;
  }

  public void setSourceSnapshotId( String sourceSnapshotId ) {
    this.sourceSnapshotId = sourceSnapshotId;
  }
}
