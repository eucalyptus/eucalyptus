/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeVpcEndpointServiceConfigurationsResponseType extends VpcMessage {


  private String nextToken;
  private ServiceConfigurationSet serviceConfigurations;

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

  public ServiceConfigurationSet getServiceConfigurations( ) {
    return serviceConfigurations;
  }

  public void setServiceConfigurations( final ServiceConfigurationSet serviceConfigurations ) {
    this.serviceConfigurations = serviceConfigurations;
  }

}
