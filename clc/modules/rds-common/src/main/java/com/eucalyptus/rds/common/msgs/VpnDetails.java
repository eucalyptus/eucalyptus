/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class VpnDetails extends EucalyptusData {

  private String vpnGatewayIp;

  private String vpnId;

  private String vpnName;

  private String vpnPSK;

  private String vpnState;

  private String vpnTunnelOriginatorIP;

  public String getVpnGatewayIp() {
    return vpnGatewayIp;
  }

  public void setVpnGatewayIp(final String vpnGatewayIp) {
    this.vpnGatewayIp = vpnGatewayIp;
  }

  public String getVpnId() {
    return vpnId;
  }

  public void setVpnId(final String vpnId) {
    this.vpnId = vpnId;
  }

  public String getVpnName() {
    return vpnName;
  }

  public void setVpnName(final String vpnName) {
    this.vpnName = vpnName;
  }

  public String getVpnPSK() {
    return vpnPSK;
  }

  public void setVpnPSK(final String vpnPSK) {
    this.vpnPSK = vpnPSK;
  }

  public String getVpnState() {
    return vpnState;
  }

  public void setVpnState(final String vpnState) {
    this.vpnState = vpnState;
  }

  public String getVpnTunnelOriginatorIP() {
    return vpnTunnelOriginatorIP;
  }

  public void setVpnTunnelOriginatorIP(final String vpnTunnelOriginatorIP) {
    this.vpnTunnelOriginatorIP = vpnTunnelOriginatorIP;
  }

}
