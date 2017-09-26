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

public class CreateVpnConnectionType extends VpcMessage {

  private String type;
  private String customerGatewayId;
  private String vpnGatewayId;
  private VpnConnectionOptionsRequestType options;

  public String getType( ) {
    return type;
  }

  public void setType( String type ) {
    this.type = type;
  }

  public String getCustomerGatewayId( ) {
    return customerGatewayId;
  }

  public void setCustomerGatewayId( String customerGatewayId ) {
    this.customerGatewayId = customerGatewayId;
  }

  public String getVpnGatewayId( ) {
    return vpnGatewayId;
  }

  public void setVpnGatewayId( String vpnGatewayId ) {
    this.vpnGatewayId = vpnGatewayId;
  }

  public VpnConnectionOptionsRequestType getOptions( ) {
    return options;
  }

  public void setOptions( VpnConnectionOptionsRequestType options ) {
    this.options = options;
  }
}
