/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class ModifyVpcEndpointServiceConfigurationType extends ComputeMessage {

  private Boolean acceptanceRequired;
  private ValueStringList addNetworkLoadBalancerArns;
  private ValueStringList removeNetworkLoadBalancerArns;
  @Nonnull
  private String serviceId;

  public Boolean getAcceptanceRequired( ) {
    return acceptanceRequired;
  }

  public void setAcceptanceRequired( final Boolean acceptanceRequired ) {
    this.acceptanceRequired = acceptanceRequired;
  }

  public ValueStringList getAddNetworkLoadBalancerArns( ) {
    return addNetworkLoadBalancerArns;
  }

  public void setAddNetworkLoadBalancerArns( final ValueStringList addNetworkLoadBalancerArns ) {
    this.addNetworkLoadBalancerArns = addNetworkLoadBalancerArns;
  }

  public ValueStringList getRemoveNetworkLoadBalancerArns( ) {
    return removeNetworkLoadBalancerArns;
  }

  public void setRemoveNetworkLoadBalancerArns( final ValueStringList removeNetworkLoadBalancerArns ) {
    this.removeNetworkLoadBalancerArns = removeNetworkLoadBalancerArns;
  }

  public String getServiceId( ) {
    return serviceId;
  }

  public void setServiceId( final String serviceId ) {
    this.serviceId = serviceId;
  }

}
