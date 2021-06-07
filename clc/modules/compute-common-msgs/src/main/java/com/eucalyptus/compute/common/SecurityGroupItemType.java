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
    return new CompatFunction<SecurityGroupItemType, String>( ) {
      @Override
      public String apply( final SecurityGroupItemType securityGroupItemType ) {
        return securityGroupItemType.getGroupId( );
      }
    };
  }

  public static CompatFunction<SecurityGroupItemType, String> groupName( ) {
    return new CompatFunction<SecurityGroupItemType, String>( ) {
      @Override
      public String apply( final SecurityGroupItemType securityGroupItemType ) {
        return securityGroupItemType.getGroupName( );
      }
    };
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
