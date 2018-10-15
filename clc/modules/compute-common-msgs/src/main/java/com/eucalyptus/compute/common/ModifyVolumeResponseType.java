/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class ModifyVolumeResponseType extends ComputeMessage {

  private VolumeModification volumeModification;

  public VolumeModification getVolumeModification( ) {
    return volumeModification;
  }

  public void setVolumeModification( final VolumeModification volumeModification ) {
    this.volumeModification = volumeModification;
  }

}
