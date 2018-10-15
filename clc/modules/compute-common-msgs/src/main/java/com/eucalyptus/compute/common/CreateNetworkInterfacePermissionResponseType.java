/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class CreateNetworkInterfacePermissionResponseType extends VpcMessage {


  private NetworkInterfacePermission interfacePermission;

  public NetworkInterfacePermission getInterfacePermission( ) {
    return interfacePermission;
  }

  public void setInterfacePermission( final NetworkInterfacePermission interfacePermission ) {
    this.interfacePermission = interfacePermission;
  }

}
