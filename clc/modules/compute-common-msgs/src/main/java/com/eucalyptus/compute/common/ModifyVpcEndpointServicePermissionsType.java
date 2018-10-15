/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class ModifyVpcEndpointServicePermissionsType extends ComputeMessage {

  private ValueStringList addAllowedPrincipals;
  private ValueStringList removeAllowedPrincipals;
  @Nonnull
  private String serviceId;

  public ValueStringList getAddAllowedPrincipals( ) {
    return addAllowedPrincipals;
  }

  public void setAddAllowedPrincipals( final ValueStringList addAllowedPrincipals ) {
    this.addAllowedPrincipals = addAllowedPrincipals;
  }

  public ValueStringList getRemoveAllowedPrincipals( ) {
    return removeAllowedPrincipals;
  }

  public void setRemoveAllowedPrincipals( final ValueStringList removeAllowedPrincipals ) {
    this.removeAllowedPrincipals = removeAllowedPrincipals;
  }

  public String getServiceId( ) {
    return serviceId;
  }

  public void setServiceId( final String serviceId ) {
    this.serviceId = serviceId;
  }

}
