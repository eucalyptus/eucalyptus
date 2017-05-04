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
 ************************************************************************/package com.eucalyptus.cluster.service.vm;

import java.util.Objects;

/**
 *
 */
public final class VmInterface {
  private final String interfaceId;
  private final String attachmentId;
  private final Integer device;
  private final String mac;
  private final String privateAddress;
  private volatile String publicAddress;

  public VmInterface(
      final String interfaceId,
      final String attachmentId,
      final Integer device,
      final String mac,
      final String privateAddress,
      final String publicAddress
  ) {
    this.interfaceId = interfaceId;
    this.attachmentId = attachmentId;
    this.device = device;
    this.mac = mac;
    this.privateAddress = privateAddress;
    this.publicAddress = Objects.toString( publicAddress, "0.0.0.0" );
  }

  public void assignPublic( String publicAddress ) {
    this.publicAddress = Objects.toString( publicAddress, "0.0.0.0" );
  }

  public String getAttachmentId() {
    return attachmentId;
  }

  public Integer getDevice() {
    return device;
  }

  public String getInterfaceId() {
    return interfaceId;
  }

  public String getMac() {
    return mac;
  }

  public String getPrivateAddress() {
    return privateAddress;
  }

  public String getPublicAddress() {
    return publicAddress;
  }
}
