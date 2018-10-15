/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ResponseError extends EucalyptusData {

  private String code;
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
