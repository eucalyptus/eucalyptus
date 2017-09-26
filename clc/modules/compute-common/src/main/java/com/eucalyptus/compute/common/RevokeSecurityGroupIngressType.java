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
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;

public class RevokeSecurityGroupIngressType extends VmSecurityMessage {

  private String sourceSecurityGroupName;
  private String sourceSecurityGroupOwnerId;
  private String ipProtocol;
  private Integer fromPort;
  private Integer toPort;
  private String cidrIp;
  @HttpParameterMapping( parameter = "UserId" )
  private String groupUserId;
  private String groupName;
  private String groupId;
  @HttpEmbedded( multiple = true )
  private ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>( );

  public String getSourceSecurityGroupName( ) {
    return sourceSecurityGroupName;
  }

  public void setSourceSecurityGroupName( String sourceSecurityGroupName ) {
    this.sourceSecurityGroupName = sourceSecurityGroupName;
  }

  public String getSourceSecurityGroupOwnerId( ) {
    return sourceSecurityGroupOwnerId;
  }

  public void setSourceSecurityGroupOwnerId( String sourceSecurityGroupOwnerId ) {
    this.sourceSecurityGroupOwnerId = sourceSecurityGroupOwnerId;
  }

  public String getIpProtocol( ) {
    return ipProtocol;
  }

  public void setIpProtocol( String ipProtocol ) {
    this.ipProtocol = ipProtocol;
  }

  public Integer getFromPort( ) {
    return fromPort;
  }

  public void setFromPort( Integer fromPort ) {
    this.fromPort = fromPort;
  }

  public Integer getToPort( ) {
    return toPort;
  }

  public void setToPort( Integer toPort ) {
    this.toPort = toPort;
  }

  public String getCidrIp( ) {
    return cidrIp;
  }

  public void setCidrIp( String cidrIp ) {
    this.cidrIp = cidrIp;
  }

  public String getGroupUserId( ) {
    return groupUserId;
  }

  public void setGroupUserId( String groupUserId ) {
    this.groupUserId = groupUserId;
  }

  public String getGroupName( ) {
    return groupName;
  }

  public void setGroupName( String groupName ) {
    this.groupName = groupName;
  }

  public String getGroupId( ) {
    return groupId;
  }

  public void setGroupId( String groupId ) {
    this.groupId = groupId;
  }

  public ArrayList<IpPermissionType> getIpPermissions( ) {
    return ipPermissions;
  }

  public void setIpPermissions( ArrayList<IpPermissionType> ipPermissions ) {
    this.ipPermissions = ipPermissions;
  }
}
