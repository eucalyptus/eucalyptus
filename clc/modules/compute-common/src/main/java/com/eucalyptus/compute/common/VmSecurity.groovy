/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

@GroovyAddClassUUID
package com.eucalyptus.compute.common;

import com.eucalyptus.auth.policy.annotation.PolicyResourceType
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
  @Override
  Boolean get_return( final boolean ifError ) {
    ifError || get_return( ) // false return should not be treated as an error, caller should check
  }
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
  @Override
  Boolean get_return( final boolean ifError ) {
    ifError || get_return( ) // false return should not be treated as an error, caller should check
  }
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
