/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class NetworkInterfacePermission extends EucalyptusData {

  private String awsAccountId;
  private String awsService;
  private String networkInterfaceId;
  private String networkInterfacePermissionId;
  private String permission;
  private NetworkInterfacePermissionState permissionState;

  public String getAwsAccountId( ) {
    return awsAccountId;
  }

  public void setAwsAccountId( final String awsAccountId ) {
    this.awsAccountId = awsAccountId;
  }

  public String getAwsService( ) {
    return awsService;
  }

  public void setAwsService( final String awsService ) {
    this.awsService = awsService;
  }

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( final String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public String getNetworkInterfacePermissionId( ) {
    return networkInterfacePermissionId;
  }

  public void setNetworkInterfacePermissionId( final String networkInterfacePermissionId ) {
    this.networkInterfacePermissionId = networkInterfacePermissionId;
  }

  public String getPermission( ) {
    return permission;
  }

  public void setPermission( final String permission ) {
    this.permission = permission;
  }

  public NetworkInterfacePermissionState getPermissionState( ) {
    return permissionState;
  }

  public void setPermissionState( final NetworkInterfacePermissionState permissionState ) {
    this.permissionState = permissionState;
  }

}
