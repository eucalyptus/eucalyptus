/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CreateCustomAvailabilityZoneType extends RdsMessage {

  @Nonnull
  private String customAvailabilityZoneName;

  private String existingVpnId;

  private String newVpnTunnelName;

  private String vpnTunnelOriginatorIP;

  public String getCustomAvailabilityZoneName() {
    return customAvailabilityZoneName;
  }

  public void setCustomAvailabilityZoneName(final String customAvailabilityZoneName) {
    this.customAvailabilityZoneName = customAvailabilityZoneName;
  }

  public String getExistingVpnId() {
    return existingVpnId;
  }

  public void setExistingVpnId(final String existingVpnId) {
    this.existingVpnId = existingVpnId;
  }

  public String getNewVpnTunnelName() {
    return newVpnTunnelName;
  }

  public void setNewVpnTunnelName(final String newVpnTunnelName) {
    this.newVpnTunnelName = newVpnTunnelName;
  }

  public String getVpnTunnelOriginatorIP() {
    return vpnTunnelOriginatorIP;
  }

  public void setVpnTunnelOriginatorIP(final String vpnTunnelOriginatorIP) {
    this.vpnTunnelOriginatorIP = vpnTunnelOriginatorIP;
  }

}
