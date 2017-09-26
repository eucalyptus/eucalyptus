/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.compute.common;

import java.util.ArrayList;

public class CreateVpcEndpointType extends VpcMessage {

  private String clientToken;
  private String policyDocument;
  private ArrayList<String> routeTableId;
  private String serviceName;
  private String vpcId;

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( String clientToken ) {
    this.clientToken = clientToken;
  }

  public String getPolicyDocument( ) {
    return policyDocument;
  }

  public void setPolicyDocument( String policyDocument ) {
    this.policyDocument = policyDocument;
  }

  public ArrayList<String> getRouteTableId( ) {
    return routeTableId;
  }

  public void setRouteTableId( ArrayList<String> routeTableId ) {
    this.routeTableId = routeTableId;
  }

  public String getServiceName( ) {
    return serviceName;
  }

  public void setServiceName( String serviceName ) {
    this.serviceName = serviceName;
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
  }
}
