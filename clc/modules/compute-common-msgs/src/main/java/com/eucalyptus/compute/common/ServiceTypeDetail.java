/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
