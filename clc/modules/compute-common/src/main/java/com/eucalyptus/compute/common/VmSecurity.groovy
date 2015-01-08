/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

@GroovyAddClassUUID
package com.eucalyptus.compute.common;

import com.eucalyptus.auth.policy.PolicyResourceType
import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping
import com.google.common.base.Function
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

@PolicyResourceType( "securitygroup" )
public class VmSecurityMessage extends ComputeMessage{
  
  public VmSecurityMessage( ) {
    super( );
  }
  
  public VmSecurityMessage( ComputeMessage msg ) {
    super( msg );
  }
  
  public VmSecurityMessage( String userId ) {
    super( userId );
  }
}
/** *******************************************************************************/
public class AuthorizeSecurityGroupIngressResponseType extends VmSecurityMessage {
}
public class AuthorizeSecurityGroupIngressType extends VmSecurityMessage {
  // Some old fields
  String sourceSecurityGroupName;
  String sourceSecurityGroupOwnerId;
  String ipProtocol;
  Integer fromPort;
  Integer toPort;
  String cidrIp;

  @HttpParameterMapping(parameter="UserId")
  String groupUserId;
  String groupName;
  String groupId;
  @HttpEmbedded( multiple=true )
  ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>();
}
/** *******************************************************************************/
class AuthorizeSecurityGroupEgressType extends VmSecurityMessage {
  String groupId;
  @HttpEmbedded( multiple=true )
  ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>();
}
class AuthorizeSecurityGroupEgressResponseType extends VmSecurityMessage {
}
/** *******************************************************************************/
public class CreateSecurityGroupResponseType extends VmSecurityMessage {
  String groupId;
}
public class CreateSecurityGroupType extends VmSecurityMessage {
  String groupName;
  String groupDescription;
  String vpcId
}
/** *******************************************************************************/
public class DeleteSecurityGroupResponseType extends VmSecurityMessage {
}
public class DeleteSecurityGroupType extends VmSecurityMessage {
  String groupName;
  String groupId;
}
/** *******************************************************************************/
public class RevokeSecurityGroupIngressResponseType extends VmSecurityMessage {
}
public class RevokeSecurityGroupIngressType extends VmSecurityMessage {
  // Some old fields
  String sourceSecurityGroupName;
  String sourceSecurityGroupOwnerId;
  String ipProtocol;
  Integer fromPort;
  Integer toPort;
  String cidrIp;

  @HttpParameterMapping(parameter="UserId")
  String groupUserId;
  String groupName;
  String groupId;
  @HttpEmbedded( multiple=true )
  ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>();
}
/** *******************************************************************************/
class RevokeSecurityGroupEgressType extends VmSecurityMessage {
  String groupId;
  @HttpEmbedded( multiple=true )
  ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>();
}
class RevokeSecurityGroupEgressResponseType extends VmSecurityMessage {
}
/** *******************************************************************************/
public class DescribeSecurityGroupsResponseType extends VmSecurityMessage {
  ArrayList<SecurityGroupItemType> securityGroupInfo = new ArrayList<SecurityGroupItemType>();
}
public class DescribeSecurityGroupsType extends VmSecurityMessage {
  @HttpParameterMapping (parameter = "GroupName")
  ArrayList<String> securityGroupSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "GroupId")
  ArrayList<String> securityGroupIdSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}
public class SecurityGroupItemType extends EucalyptusData {
  String accountId;
  String groupName;
  String groupDescription;
  String groupId;
  String vpcId;
  ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>();
  ArrayList<IpPermissionType> ipPermissionsEgress = new ArrayList<IpPermissionType>();
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>();
  
  public SecurityGroupItemType( ) {
    super( );
  }
  public SecurityGroupItemType( String accountId, String groupId, String groupName, String groupDescription, String vpcId ) {
    super( );
    this.accountId = accountId;
    this.groupId = groupId;
    this.groupName = groupName;
    this.groupDescription = groupDescription;
    this.vpcId = vpcId
  }

  static Function<SecurityGroupItemType,String> groupId( ) {
    { SecurityGroupItemType item -> item.groupId } as Function<SecurityGroupItemType,String>
  }

  static Function<SecurityGroupItemType,String> groupName( ) {
    { SecurityGroupItemType item -> item.groupName } as Function<SecurityGroupItemType,String>
  }
}

public class IpPermissionType extends EucalyptusData {
  String ipProtocol;
  Integer fromPort;
  Integer toPort;
  @HttpEmbedded( multiple=true )
  ArrayList<UserIdGroupPairType> groups = new ArrayList<UserIdGroupPairType>();
  @HttpEmbedded( multiple=true )
  ArrayList<CidrIpType> ipRanges = new ArrayList<CidrIpType>();
  
  def IpPermissionType(){
  }
  
  def IpPermissionType(String ipProtocol, Integer fromPort, Integer toPort ) {
    this.ipProtocol = ipProtocol;
    this.fromPort = fromPort;
    this.toPort = toPort;
  }

  List<String> getCidrIpRanges( ) {
    ipRanges.collect{ CidrIpType cidrIp -> cidrIp.getCidrIp() }
  }

  void setCidrIpRanges( Collection<String> cidrIps ) {
    ipRanges = cidrIps.collect{ String cidrIp -> new CidrIpType( cidrIp: cidrIp ) } as ArrayList<CidrIpType>
  }
}
public class UserIdGroupPairType extends EucalyptusData {
  @HttpParameterMapping( parameter="UserId" )
  String sourceUserId;
  @HttpParameterMapping( parameter="GroupName" )
  String sourceGroupName;
  @HttpParameterMapping( parameter="GroupId" )
  String sourceGroupId;

  def UserIdGroupPairType(){
  }
  
  def UserIdGroupPairType( String userId, String groupName, String groupId ) {
    this.sourceUserId = userId
    this.sourceGroupName = groupName
    this.sourceGroupId = groupId
  }
}

public class CidrIpType extends EucalyptusData {
  String cidrIp
}
