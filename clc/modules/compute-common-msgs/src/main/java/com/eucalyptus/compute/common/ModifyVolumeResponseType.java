/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
