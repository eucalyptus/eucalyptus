/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import javax.annotation.Nonnull;


public class UnsuccessfulItemError extends EucalyptusData {

  @Nonnull
  private String code;
  @Nonnull
  private String message;

  public String getCode( ) {
    return code;
  }

  public void setCode( final String code ) {
    this.code = code;
  }

  public String getMessage( ) {
    return message;
  }

  public void setMessage( final String message ) {
    this.message = message;
  }

}
