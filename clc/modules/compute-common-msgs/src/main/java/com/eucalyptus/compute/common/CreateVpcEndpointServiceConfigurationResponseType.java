/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class CreateVpcEndpointServiceConfigurationResponseType extends VpcMessage {


  private String clientToken;
  private ServiceConfiguration serviceConfiguration;

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( final String clientToken ) {
    this.clientToken = clientToken;
  }

  public ServiceConfiguration getServiceConfiguration( ) {
    return serviceConfiguration;
  }

  public void setServiceConfiguration( final ServiceConfiguration serviceConfiguration ) {
    this.serviceConfiguration = serviceConfiguration;
  }

}
