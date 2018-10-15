/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ElasticGpuSpecificationResponse extends EucalyptusData {

  private String type;

  public String getType( ) {
    return type;
  }

  public void setType( final String type ) {
    this.type = type;
  }

}
