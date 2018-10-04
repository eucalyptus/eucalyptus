/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
