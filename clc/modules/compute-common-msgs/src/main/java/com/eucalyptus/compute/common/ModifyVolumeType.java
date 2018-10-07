/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class ModifyVolumeType extends ComputeMessage {

  private Integer iops;
  private Integer size;
  @Nonnull
  private String volumeId;
  private String volumeType;

  public Integer getIops( ) {
    return iops;
  }

  public void setIops( final Integer iops ) {
    this.iops = iops;
  }

  public Integer getSize( ) {
    return size;
  }

  public void setSize( final Integer size ) {
    this.size = size;
  }

  public String getVolumeId( ) {
    return volumeId;
  }

  public void setVolumeId( final String volumeId ) {
    this.volumeId = volumeId;
  }

  public String getVolumeType( ) {
    return volumeType;
  }

  public void setVolumeType( final String volumeType ) {
    this.volumeType = volumeType;
  }

}
