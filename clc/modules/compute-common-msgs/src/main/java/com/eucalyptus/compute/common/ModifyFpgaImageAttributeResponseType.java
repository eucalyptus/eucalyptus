/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class ModifyFpgaImageAttributeResponseType extends ComputeMessage {

  private FpgaImageAttribute fpgaImageAttribute;

  public FpgaImageAttribute getFpgaImageAttribute( ) {
    return fpgaImageAttribute;
  }

  public void setFpgaImageAttribute( final FpgaImageAttribute fpgaImageAttribute ) {
    this.fpgaImageAttribute = fpgaImageAttribute;
  }

}
