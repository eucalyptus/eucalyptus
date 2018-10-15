/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class CreateFpgaImageResponseType extends ComputeMessage {

  private String fpgaImageGlobalId;
  private String fpgaImageId;

  public String getFpgaImageGlobalId( ) {
    return fpgaImageGlobalId;
  }

  public void setFpgaImageGlobalId( final String fpgaImageGlobalId ) {
    this.fpgaImageGlobalId = fpgaImageGlobalId;
  }

  public String getFpgaImageId( ) {
    return fpgaImageId;
  }

  public void setFpgaImageId( final String fpgaImageId ) {
    this.fpgaImageId = fpgaImageId;
  }

}
