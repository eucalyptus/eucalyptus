/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DescribeNetworkInterfacePermissionsResponseType extends VpcMessage {


  private NetworkInterfacePermissionList networkInterfacePermissions;
  private String nextToken;

  public NetworkInterfacePermissionList getNetworkInterfacePermissions( ) {
    return networkInterfacePermissions;
  }

  public void setNetworkInterfacePermissions( final NetworkInterfacePermissionList networkInterfacePermissions ) {
    this.networkInterfacePermissions = networkInterfacePermissions;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( final String nextToken ) {
    this.nextToken = nextToken;
  }

}
