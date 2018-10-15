/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ServiceTypeDetail extends EucalyptusData {

  private String serviceType;

  public String getServiceType( ) {
    return serviceType;
  }

  public void setServiceType( final String serviceType ) {
    this.serviceType = serviceType;
  }

}
