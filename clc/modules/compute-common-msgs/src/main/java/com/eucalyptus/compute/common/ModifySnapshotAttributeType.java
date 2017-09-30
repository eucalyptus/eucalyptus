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
