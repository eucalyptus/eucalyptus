/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
