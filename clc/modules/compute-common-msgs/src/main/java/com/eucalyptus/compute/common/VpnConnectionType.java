/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
