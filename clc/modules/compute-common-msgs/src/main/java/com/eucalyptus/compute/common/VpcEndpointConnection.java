/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;



public class VpcEndpointConnection extends EucalyptusData {

  private java.util.Date creationTimestamp;
  private String serviceId;
  private String vpcEndpointId;
  private String vpcEndpointOwner;
  private String vpcEndpointState;

  public java.util.Date getCreationTimestamp( ) {
    return creationTimestamp;
  }

  public void setCreationTimestamp( final java.util.Date creationTimestamp ) {
    this.creationTimestamp = creationTimestamp;
  }

  public String getServiceId( ) {
    return serviceId;
  }

  public void setServiceId( final String serviceId ) {
    this.serviceId = serviceId;
  }

  public String getVpcEndpointId( ) {
    return vpcEndpointId;
  }

  public void setVpcEndpointId( final String vpcEndpointId ) {
    this.vpcEndpointId = vpcEndpointId;
  }

  public String getVpcEndpointOwner( ) {
    return vpcEndpointOwner;
  }

  public void setVpcEndpointOwner( final String vpcEndpointOwner ) {
    this.vpcEndpointOwner = vpcEndpointOwner;
  }

  public String getVpcEndpointState( ) {
    return vpcEndpointState;
  }

  public void setVpcEndpointState( final String vpcEndpointState ) {
    this.vpcEndpointState = vpcEndpointState;
  }

}
