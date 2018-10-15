/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class CreateVpcEndpointServiceConfigurationType extends VpcMessage {

  private Boolean acceptanceRequired;
  private String clientToken;
  @Nonnull
  private ValueStringList networkLoadBalancerArns;

  public Boolean getAcceptanceRequired( ) {
    return acceptanceRequired;
  }

  public void setAcceptanceRequired( final Boolean acceptanceRequired ) {
    this.acceptanceRequired = acceptanceRequired;
  }

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( final String clientToken ) {
    this.clientToken = clientToken;
  }

  public ValueStringList getNetworkLoadBalancerArns( ) {
    return networkLoadBalancerArns;
  }

  public void setNetworkLoadBalancerArns( final ValueStringList networkLoadBalancerArns ) {
    this.networkLoadBalancerArns = networkLoadBalancerArns;
  }

}
