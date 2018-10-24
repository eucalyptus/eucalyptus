/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class DescribeFpgaImageAttributeType extends ComputeMessage {

  @Nonnull
  private String attribute;
  @Nonnull
  private String fpgaImageId;

  public String getAttribute( ) {
    return attribute;
  }

  public void setAttribute( final String attribute ) {
    this.attribute = attribute;
  }

  public String getFpgaImageId( ) {
    return fpgaImageId;
  }

  public void setFpgaImageId( final String fpgaImageId ) {
    this.fpgaImageId = fpgaImageId;
  }

}
