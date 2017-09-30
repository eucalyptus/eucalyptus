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

public class ModifyVpcEndpointType extends VpcMessage {

  private ArrayList<String> addRouteTableId;
  private String policyDocument;
  private ArrayList<String> removeRouteTableId;
  private Boolean resetPolicy;
  private String vpcEndpointId;

  public ArrayList<String> getAddRouteTableId( ) {
    return addRouteTableId;
  }

  public void setAddRouteTableId( ArrayList<String> addRouteTableId ) {
    this.addRouteTableId = addRouteTableId;
  }

  public String getPolicyDocument( ) {
    return policyDocument;
  }

  public void setPolicyDocument( String policyDocument ) {
    this.policyDocument = policyDocument;
  }

  public ArrayList<String> getRemoveRouteTableId( ) {
    return removeRouteTableId;
  }

  public void setRemoveRouteTableId( ArrayList<String> removeRouteTableId ) {
    this.removeRouteTableId = removeRouteTableId;
  }

  public Boolean getResetPolicy( ) {
    return resetPolicy;
  }

  public void setResetPolicy( Boolean resetPolicy ) {
    this.resetPolicy = resetPolicy;
  }

  public String getVpcEndpointId( ) {
    return vpcEndpointId;
  }

  public void setVpcEndpointId( String vpcEndpointId ) {
    this.vpcEndpointId = vpcEndpointId;
  }
}
