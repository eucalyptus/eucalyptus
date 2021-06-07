/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpParameterMapping;


public class AcceptVpcEndpointConnectionsType extends VpcMessage {

  @Nonnull
  private String serviceId;
  @HttpParameterMapping( parameter = "VpcEndpointId" )
  @Nonnull
  private ValueStringList vpcEndpointIds;

  public String getServiceId( ) {
    return serviceId;
  }

  public void setServiceId( final String serviceId ) {
    this.serviceId = serviceId;
  }

  public ValueStringList getVpcEndpointIds( ) {
    return vpcEndpointIds;
  }

  public void setVpcEndpointIds( final ValueStringList vpcEndpointIds ) {
    this.vpcEndpointIds = vpcEndpointIds;
  }

}
