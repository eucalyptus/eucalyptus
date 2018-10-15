/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class DeleteNetworkInterfacePermissionType extends VpcMessage {

  private Boolean force;
  @Nonnull
  private String networkInterfacePermissionId;

  public Boolean getForce( ) {
    return force;
  }

  public void setForce( final Boolean force ) {
    this.force = force;
  }

  public String getNetworkInterfacePermissionId( ) {
    return networkInterfacePermissionId;
  }

  public void setNetworkInterfacePermissionId( final String networkInterfacePermissionId ) {
    this.networkInterfacePermissionId = networkInterfacePermissionId;
  }

}
