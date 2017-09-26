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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class VpnConnectionType extends EucalyptusData {

  private String vpnConnectionId;
  private String state;
  private String customerGatewayConfiguration;
  private String type;
  private String customerGatewayId;
  private String vpnGatewayId;
  private ResourceTagSetType tagSet;
  private VgwTelemetryType vgwTelemetry;
  private VpnConnectionOptionsResponseType options;
  private VpnStaticRoutesSetType routes;

  public String getVpnConnectionId( ) {
    return vpnConnectionId;
  }

  public void setVpnConnectionId( String vpnConnectionId ) {
    this.vpnConnectionId = vpnConnectionId;
  }

  public String getState( ) {
    return state;
  }

  public void setState( String state ) {
    this.state = state;
  }

  public String getCustomerGatewayConfiguration( ) {
    return customerGatewayConfiguration;
  }

  public void setCustomerGatewayConfiguration( String customerGatewayConfiguration ) {
    this.customerGatewayConfiguration = customerGatewayConfiguration;
  }

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

  public ResourceTagSetType getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ResourceTagSetType tagSet ) {
    this.tagSet = tagSet;
  }

  public VgwTelemetryType getVgwTelemetry( ) {
    return vgwTelemetry;
  }

  public void setVgwTelemetry( VgwTelemetryType vgwTelemetry ) {
    this.vgwTelemetry = vgwTelemetry;
  }

  public VpnConnectionOptionsResponseType getOptions( ) {
    return options;
  }

  public void setOptions( VpnConnectionOptionsResponseType options ) {
    this.options = options;
  }

  public VpnStaticRoutesSetType getRoutes( ) {
    return routes;
  }

  public void setRoutes( VpnStaticRoutesSetType routes ) {
    this.routes = routes;
  }
}
