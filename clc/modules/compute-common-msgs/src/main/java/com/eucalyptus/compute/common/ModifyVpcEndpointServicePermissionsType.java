/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
