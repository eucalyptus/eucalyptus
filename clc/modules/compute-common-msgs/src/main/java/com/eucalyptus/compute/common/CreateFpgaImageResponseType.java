/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
