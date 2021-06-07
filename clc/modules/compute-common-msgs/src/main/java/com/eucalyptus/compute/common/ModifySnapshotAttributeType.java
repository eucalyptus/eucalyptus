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
import java.util.List;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.util.StreamUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class ModifySnapshotAttributeType extends BlockSnapshotMessage {

  private String snapshotId;
  @HttpParameterMapping( parameter = "ProductCode" )
  private ArrayList<String> productCodes = Lists.newArrayList( );
  private CreateVolumePermissionOperationType createVolumePermission;
  @HttpParameterMapping( parameter = "UserId" )
  private ArrayList<String> queryUserId = Lists.newArrayList( );
  @HttpParameterMapping( parameter = { "Group" } )
  private ArrayList<String> queryUserGroup = Lists.newArrayList( );
  private String attribute;
  private String operationType;

  public SnapshotAttribute snapshotAttribute( ) {
    if ( !Strings.isNullOrEmpty( attribute ) ) {
      return "createVolumePermission".equals( attribute ) ? SnapshotAttribute.CreateVolumePermission : SnapshotAttribute.ProductCode;
    } else {
      return createVolumePermission != null && ( !createVolumePermission.getAdd( ).isEmpty( ) || !createVolumePermission.getRemove( ).isEmpty( ) ) ? SnapshotAttribute.CreateVolumePermission : !productCodes.isEmpty( ) ? SnapshotAttribute.ProductCode : null;
    }

  }

  public boolean add( ) {
    return !asAddCreateVolumePermissionsItemTypes( ).isEmpty( );
  }

  public boolean remove( ) {
    return !asRemoveCreateVolumePermissionsItemTypes( ).isEmpty( );
  }

  public List<String> addUserIds( ) {
    List<String> userIds = Lists.newArrayList( );
    if ( add( ) ) {
      for ( CreateVolumePermissionItemType item : asAddCreateVolumePermissionsItemTypes( ) ) {
        if ( !Strings.isNullOrEmpty( item.getUserId( ) ) ) {
          userIds.add( item.getUserId( ) );
        }
      }
    }
    return userIds;
  }

  public List<String> removeUserIds( ) {
    List<String> userIds = Lists.newArrayList( );
    if ( remove( ) ) {
      for ( CreateVolumePermissionItemType item : asRemoveCreateVolumePermissionsItemTypes( ) ) {
        if ( !Strings.isNullOrEmpty( item.getUserId( ) ) ) {
          userIds.add( item.getUserId( ) );
        }
      }
    }
    return userIds;
  }

  public boolean addGroupAll( ) {
    boolean all = false;
    for ( CreateVolumePermissionItemType item : asAddCreateVolumePermissionsItemTypes( ) ) {
      if ( "all".equals( item.getGroup( ) ) ) {
        all = true;
        break;
      }
    }
    return all;
  }

  public boolean removeGroupAll( ) {
    boolean all = false;
    for ( CreateVolumePermissionItemType item : asRemoveCreateVolumePermissionsItemTypes( ) ) {
      if ( "all".equals( item.getGroup( ) ) ) {
        all = true;
        break;
      }
    }
    return all;
  }

  public List<CreateVolumePermissionItemType> asAddCreateVolumePermissionsItemTypes( ) {
    return !Strings.isNullOrEmpty( attribute ) ? "add".equals( operationType ) ? asCreateVolumePermissionItemTypes( ) : Lists.<CreateVolumePermissionItemType>newArrayList( ) : createVolumePermission.getAdd( );
  }

  public List<CreateVolumePermissionItemType> asRemoveCreateVolumePermissionsItemTypes( ) {
    return !Strings.isNullOrEmpty( attribute ) ? "add".equals( operationType ) ? Lists.<CreateVolumePermissionItemType>newArrayList( ) : asCreateVolumePermissionItemTypes( ) : createVolumePermission.getRemove( );
  }

  List<CreateVolumePermissionItemType> asCreateVolumePermissionItemTypes( ) {
    return queryUserId.isEmpty( ) ? StreamUtil.ofAll( queryUserGroup ).map( CreateVolumePermissionItemType.forGroup( ) ).toJavaList( ) : StreamUtil.ofAll( queryUserId ).map( CreateVolumePermissionItemType.forUser( ) ).toJavaList( );
  }

  public String getSnapshotId( ) {
    return snapshotId;
  }

  public void setSnapshotId( String snapshotId ) {
    this.snapshotId = snapshotId;
  }

  public ArrayList<String> getProductCodes( ) {
    return productCodes;
  }

  public void setProductCodes( ArrayList<String> productCodes ) {
    this.productCodes = productCodes;
  }

  public CreateVolumePermissionOperationType getCreateVolumePermission( ) {
    return createVolumePermission;
  }

  public void setCreateVolumePermission( CreateVolumePermissionOperationType createVolumePermission ) {
    this.createVolumePermission = createVolumePermission;
  }

  public ArrayList<String> getQueryUserId( ) {
    return queryUserId;
  }

  public void setQueryUserId( ArrayList<String> queryUserId ) {
    this.queryUserId = queryUserId;
  }

  public ArrayList<String> getQueryUserGroup( ) {
    return queryUserGroup;
  }

  public void setQueryUserGroup( ArrayList<String> queryUserGroup ) {
    this.queryUserGroup = queryUserGroup;
  }

  public String getAttribute( ) {
    return attribute;
  }

  public void setAttribute( String attribute ) {
    this.attribute = attribute;
  }

  public String getOperationType( ) {
    return operationType;
  }

  public void setOperationType( String operationType ) {
    this.operationType = operationType;
  }

  public enum SnapshotAttribute {
    CreateVolumePermission, ProductCode
  }
}
