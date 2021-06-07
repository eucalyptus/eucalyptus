/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpParameterMapping;


public class DeleteVpcEndpointServiceConfigurationsType extends VpcMessage {

  @HttpParameterMapping( parameter = "ServiceId" )
  @Nonnull
  private ValueStringList serviceIds;

  public ValueStringList getServiceIds( ) {
    return serviceIds;
  }

  public void setServiceIds( final ValueStringList serviceIds ) {
    this.serviceIds = serviceIds;
  }

}
