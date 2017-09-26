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

import java.util.ArrayList;
import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class SecurityGroupItemType extends EucalyptusData {

  private String accountId;
  private String groupName;
  private String groupDescription;
  private String groupId;
  private String vpcId;
  private ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>( );
  private ArrayList<IpPermissionType> ipPermissionsEgress = new ArrayList<IpPermissionType>( );
  private ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>( );

  public SecurityGroupItemType( ) {
    super( );
  }

  public SecurityGroupItemType( String accountId, String groupId, String groupName, String groupDescription, String vpcId ) {
    super( );
    this.accountId = accountId;
    this.groupId = groupId;
    this.groupName = groupName;
    this.groupDescription = groupDescription;
    this.vpcId = vpcId;
  }

  public static CompatFunction<SecurityGroupItemType, String> groupId( ) {
    return SecurityGroupItemType::getGroupId;
  }

  public static CompatFunction<SecurityGroupItemType, String> groupName( ) {
    return SecurityGroupItemType::getGroupName;
  }

  public String getAccountId( ) {
    return accountId;
  }

  public void setAccountId( String accountId ) {
    this.accountId = accountId;
  }

  public String getGroupName( ) {
    return groupName;
  }

  public void setGroupName( String groupName ) {
    this.groupName = groupName;
  }

  public String getGroupDescription( ) {
    return groupDescription;
  }

  public void setGroupDescription( String groupDescription ) {
    this.groupDescription = groupDescription;
  }

  public String getGroupId( ) {
    return groupId;
  }

  public void setGroupId( String groupId ) {
    this.groupId = groupId;
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
  }

  public ArrayList<IpPermissionType> getIpPermissions( ) {
    return ipPermissions;
  }

  public void setIpPermissions( ArrayList<IpPermissionType> ipPermissions ) {
    this.ipPermissions = ipPermissions;
  }

  public ArrayList<IpPermissionType> getIpPermissionsEgress( ) {
    return ipPermissionsEgress;
  }

  public void setIpPermissionsEgress( ArrayList<IpPermissionType> ipPermissionsEgress ) {
    this.ipPermissionsEgress = ipPermissionsEgress;
  }

  public ArrayList<ResourceTag> getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ArrayList<ResourceTag> tagSet ) {
    this.tagSet = tagSet;
  }
}
