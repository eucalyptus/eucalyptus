/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.info;

import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class AWSRDSDBInstanceResourceInfo  extends ResourceInfo {

  @AttributeJson
  private String arn;

  @AttributeJson(name="Endpoint.Address")
  private String endpointAddress;

  @AttributeJson(name="Endpoint.Port")
  private String endpointPort;

  public AWSRDSDBInstanceResourceInfo( ) {
    setType( "AWS::RDS::DBInstance" );
  }

  @Override
  public boolean supportsTags( ) {
    return true;
  }

  public String getArn() {
    return arn;
  }

  public void setArn(String arn) {
    this.arn = arn;
  }

  public String getEndpointAddress() {
    return endpointAddress;
  }

  public void setEndpointAddress(final String endpointAddress) {
    this.endpointAddress = endpointAddress;
  }

  public String getEndpointPort() {
    return endpointPort;
  }

  public void setEndpointPort(final String endpointPort) {
    this.endpointPort = endpointPort;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add( "arn", arn )
        .add("endpointAddress", endpointAddress)
        .add("endpointPort", endpointPort)
        .toString();
  }
}

