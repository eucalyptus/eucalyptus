/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ActiveInstance extends EucalyptusData {

  private String instanceHealth;
  private String instanceId;
  private String instanceType;
  private String spotInstanceRequestId;

  public String getInstanceHealth( ) {
    return instanceHealth;
  }

  public void setInstanceHealth( final String instanceHealth ) {
    this.instanceHealth = instanceHealth;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( final String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( final String instanceType ) {
    this.instanceType = instanceType;
  }

  public String getSpotInstanceRequestId( ) {
    return spotInstanceRequestId;
  }

  public void setSpotInstanceRequestId( final String spotInstanceRequestId ) {
    this.spotInstanceRequestId = spotInstanceRequestId;
  }

}
