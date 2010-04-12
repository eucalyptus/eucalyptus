/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.msgs

import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;

public class VmSecurityMessage extends EucalyptusMessage{}
/** *******************************************************************************/
public class AuthorizeSecurityGroupIngressResponseType extends VmSecurityMessage {
  boolean _return;
}
public class AuthorizeSecurityGroupIngressType extends VmSecurityMessage {
  @HttpParameterMapping(parameter="UserId")
  String groupUserId;
  String groupName;
  @HttpEmbedded
  ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>();
}
/** *******************************************************************************/
public class CreateSecurityGroupResponseType extends VmSecurityMessage {
  boolean _return;
}
public class CreateSecurityGroupType extends VmSecurityMessage {
  String groupName;
  String groupDescription;
}
/** *******************************************************************************/
public class DeleteSecurityGroupResponseType extends VmSecurityMessage {
  boolean _return;
}
public class DeleteSecurityGroupType extends VmSecurityMessage {
  String groupName;
}
/** *******************************************************************************/
public class RevokeSecurityGroupIngressResponseType extends VmSecurityMessage {
  boolean _return;
}
public class RevokeSecurityGroupIngressType extends VmSecurityMessage {
  @HttpParameterMapping(parameter="UserId")
  String groupUserId;
  String groupName;
  @HttpEmbedded
  ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>();
}
/** *******************************************************************************/
public class DescribeSecurityGroupsResponseType extends VmSecurityMessage {
  ArrayList<SecurityGroupItemType> securityGroupInfo = new ArrayList<SecurityGroupItemType>();
}
public class DescribeSecurityGroupsType extends VmSecurityMessage {
  @HttpParameterMapping (parameter = "GroupName")
  ArrayList<String> securityGroupSet = new ArrayList<String>();
}
public class SecurityGroupItemType extends EucalyptusData {
  String ownerId;
  String groupName;
  String groupDescription;
  ArrayList<IpPermissionType> ipPermissions = new ArrayList<IpPermissionType>();
}

public class IpPermissionType extends EucalyptusData {
  String ipProtocol;
  int fromPort;
  int toPort;
  @HttpEmbedded
  ArrayList<UserIdGroupPairType> groups = new ArrayList<UserIdGroupPairType>();
  @HttpParameterMapping(parameter = "CidrIp")
  ArrayList<String> ipRanges = new ArrayList<String>();

  def IpPermissionType(){}

  def IpPermissionType(final ipProtocol, final fromPort, final toPort )
  {
    this.ipProtocol = ipProtocol;
    this.fromPort = fromPort;
    this.toPort = toPort;
  }

}
public class UserIdGroupPairType extends EucalyptusData {
  @HttpParameterMapping(parameter = "SourceSecurityGroupOwnerId")
  String sourceUserId;
  @HttpParameterMapping(parameter = "SourceSecurityGroupName")
  String sourceGroupName;

  def UserIdGroupPairType(){}

  def UserIdGroupPairType(final sourceUserId, final sourceGroupName)
  {
    this.sourceUserId = sourceUserId;
    this.sourceGroupName = sourceGroupName;
  }



}
