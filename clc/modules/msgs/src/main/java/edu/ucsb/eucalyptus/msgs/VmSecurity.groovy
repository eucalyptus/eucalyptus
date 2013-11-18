/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

@GroovyAddClassUUID
package edu.ucsb.eucalyptus.msgs

import com.eucalyptus.auth.policy.PolicyResourceType
import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping

@PolicyResourceType( "securitygroup" )
public class VmSecurityMessage extends EucalyptusMessage{
  
  public VmSecurityMessage( ) {
    super( );
  }
  
  public VmSecurityMessage( EucalyptusMessage msg ) {
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
public class CreateSecurityGroupResponseType extends VmSecurityMessage {
  String groupId;
}
public class CreateSecurityGroupType extends VmSecurityMessage {
  String groupName;
  String groupDescription;
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
  ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>();
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>();
  
  public SecurityGroupItemType( ) {
    super( );
  }
  public SecurityGroupItemType( String accountId, String groupId, String groupName, String groupDescription ) {
    super( );
    this.accountId = accountId;
    this.groupId = groupId;
    this.groupName = groupName;
    this.groupDescription = groupDescription;
  }
}

public class IpPermissionType extends EucalyptusData {
  String ipProtocol;
  int fromPort;
  int toPort;
  @HttpEmbedded( multiple=true )
  ArrayList<UserIdGroupPairType> groups = new ArrayList<UserIdGroupPairType>();
  @HttpEmbedded( multiple=true )
  ArrayList<CidrIpType> ipRanges = new ArrayList<CidrIpType>();
  
  def IpPermissionType(){
  }
  
  def IpPermissionType(final ipProtocol, final fromPort, final toPort ) {
    this.ipProtocol = ipProtocol;
    this.fromPort = fromPort;
    this.toPort = toPort;
  }

  List<String> getCidrIpRanges( ) {
    ipRanges.collect{ cidrIp -> cidrIp.getCidrIp() }
  }

  void setCidrIpRanges( Collection<String> cidrIps ) {
    ipRanges = cidrIps.collect{ cidrIp -> new CidrIpType( cidrIp: cidrIp ) }
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
