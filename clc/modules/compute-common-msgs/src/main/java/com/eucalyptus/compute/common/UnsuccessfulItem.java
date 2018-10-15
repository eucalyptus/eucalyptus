/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import javax.annotation.Nonnull;


public class UnsuccessfulItem extends EucalyptusData {

  @Nonnull
  private UnsuccessfulItemError error;
  private String resourceId;

  public UnsuccessfulItemError getError( ) {
    return error;
  }

  public void setError( final UnsuccessfulItemError error ) {
    this.error = error;
  }

  public String getResourceId( ) {
    return resourceId;
  }

  public void setResourceId( final String resourceId ) {
    this.resourceId = resourceId;
  }

}
