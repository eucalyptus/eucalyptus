/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;


public class UpdateSecurityGroupRuleDescriptionsIngressType extends ComputeMessage {

  private String groupId;
  private String groupName;
  @HttpEmbedded( multiple = true )
  private ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>( );

  public String getGroupId( ) {
    return groupId;
  }

  public void setGroupId( final String groupId ) {
    this.groupId = groupId;
  }

  public String getGroupName( ) {
    return groupName;
  }

  public void setGroupName( final String groupName ) {
    this.groupName = groupName;
  }

  public ArrayList<IpPermissionType> getIpPermissions( ) {
    return ipPermissions;
  }

  public void setIpPermissions( ArrayList<IpPermissionType> ipPermissions ) {
    this.ipPermissions = ipPermissions;
  }

}
