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

public class ExportToS3Task extends EucalyptusData {

  private String diskImageFormat;
  private String containerFormat;
  private String s3Bucket;
  private String s3Prefix;

  public void ExportToS3TaskType( ) {
  }

  public String getDiskImageFormat( ) {
    return diskImageFormat;
  }

  public void setDiskImageFormat( String diskImageFormat ) {
    this.diskImageFormat = diskImageFormat;
  }

  public String getContainerFormat( ) {
    return containerFormat;
  }

  public void setContainerFormat( String containerFormat ) {
    this.containerFormat = containerFormat;
  }

  public String getS3Bucket( ) {
    return s3Bucket;
  }

  public void setS3Bucket( String s3Bucket ) {
    this.s3Bucket = s3Bucket;
  }

  public String getS3Prefix( ) {
    return s3Prefix;
  }

  public void setS3Prefix( String s3Prefix ) {
    this.s3Prefix = s3Prefix;
  }
}
