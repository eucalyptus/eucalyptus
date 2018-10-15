/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeVolumesModificationsResponseType extends ComputeMessage {

  private String nextToken;
  private VolumeModificationList volumesModifications;

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

  public VolumeModificationList getVolumesModifications( ) {
    return volumesModifications;
  }

  public void setVolumesModifications( final VolumeModificationList volumesModifications ) {
    this.volumesModifications = volumesModifications;
  }

}
