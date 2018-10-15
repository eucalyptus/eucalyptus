/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeFpgaImagesResponseType extends ComputeMessage {


  private FpgaImageList fpgaImages;
  private String nextToken;

  public FpgaImageList getFpgaImages( ) {
    return fpgaImages;
  }

  public void setFpgaImages( final FpgaImageList fpgaImages ) {
    this.fpgaImages = fpgaImages;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

}
