/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LaunchTemplateEbsBlockDevice extends EucalyptusData {

  private Boolean deleteOnTermination;
  private Boolean encrypted;
  private Integer iops;
  private String kmsKeyId;
  private String snapshotId;
  private Integer volumeSize;
  private String volumeType;

  public Boolean getDeleteOnTermination( ) {
    return deleteOnTermination;
  }

  public void setDeleteOnTermination( final Boolean deleteOnTermination ) {
    this.deleteOnTermination = deleteOnTermination;
  }

  public Boolean getEncrypted( ) {
    return encrypted;
  }

  public void setEncrypted( final Boolean encrypted ) {
    this.encrypted = encrypted;
  }

  public Integer getIops( ) {
    return iops;
  }

  public void setIops( final Integer iops ) {
    this.iops = iops;
  }

  public String getKmsKeyId( ) {
    return kmsKeyId;
  }

  public void setKmsKeyId( final String kmsKeyId ) {
    this.kmsKeyId = kmsKeyId;
  }

  public String getSnapshotId( ) {
    return snapshotId;
  }

  public void setSnapshotId( final String snapshotId ) {
    this.snapshotId = snapshotId;
  }

  public Integer getVolumeSize( ) {
    return volumeSize;
  }

  public void setVolumeSize( final Integer volumeSize ) {
    this.volumeSize = volumeSize;
  }

  public String getVolumeType( ) {
    return volumeType;
  }

  public void setVolumeType( final String volumeType ) {
    this.volumeType = volumeType;
  }

}
