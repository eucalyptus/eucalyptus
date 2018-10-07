/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class VolumeModification extends EucalyptusData {

  private java.util.Date endTime;
  private String modificationState;
  private Integer originalIops;
  private Integer originalSize;
  private String originalVolumeType;
  private Long progress;
  private java.util.Date startTime;
  private String statusMessage;
  private Integer targetIops;
  private Integer targetSize;
  private String targetVolumeType;
  private String volumeId;

  public java.util.Date getEndTime( ) {
    return endTime;
  }

  public void setEndTime( final java.util.Date endTime ) {
    this.endTime = endTime;
  }

  public String getModificationState( ) {
    return modificationState;
  }

  public void setModificationState( final String modificationState ) {
    this.modificationState = modificationState;
  }

  public Integer getOriginalIops( ) {
    return originalIops;
  }

  public void setOriginalIops( final Integer originalIops ) {
    this.originalIops = originalIops;
  }

  public Integer getOriginalSize( ) {
    return originalSize;
  }

  public void setOriginalSize( final Integer originalSize ) {
    this.originalSize = originalSize;
  }

  public String getOriginalVolumeType( ) {
    return originalVolumeType;
  }

  public void setOriginalVolumeType( final String originalVolumeType ) {
    this.originalVolumeType = originalVolumeType;
  }

  public Long getProgress( ) {
    return progress;
  }

  public void setProgress( final Long progress ) {
    this.progress = progress;
  }

  public java.util.Date getStartTime( ) {
    return startTime;
  }

  public void setStartTime( final java.util.Date startTime ) {
    this.startTime = startTime;
  }

  public String getStatusMessage( ) {
    return statusMessage;
  }

  public void setStatusMessage( final String statusMessage ) {
    this.statusMessage = statusMessage;
  }

  public Integer getTargetIops( ) {
    return targetIops;
  }

  public void setTargetIops( final Integer targetIops ) {
    this.targetIops = targetIops;
  }

  public Integer getTargetSize( ) {
    return targetSize;
  }

  public void setTargetSize( final Integer targetSize ) {
    this.targetSize = targetSize;
  }

  public String getTargetVolumeType( ) {
    return targetVolumeType;
  }

  public void setTargetVolumeType( final String targetVolumeType ) {
    this.targetVolumeType = targetVolumeType;
  }

  public String getVolumeId( ) {
    return volumeId;
  }

  public void setVolumeId( final String volumeId ) {
    this.volumeId = volumeId;
  }

}
