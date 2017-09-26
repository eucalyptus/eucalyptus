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
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import javaslang.collection.Stream;

public class ModifyImageAttributeType extends VmImageMessage {

  private String imageId;
  @HttpParameterMapping( parameter = "ProductCode" )
  private ArrayList<String> productCodes = Lists.newArrayList( );
  private LaunchPermissionOperationType launchPermission;
  @HttpParameterMapping( parameter = "Description.Value" )
  private String description;
  @HttpParameterMapping( parameter = "UserId" )
  private ArrayList<String> queryUserId = Lists.newArrayList( );
  @HttpParameterMapping( parameter = { "Group", "UserGroup" } )
  private ArrayList<String> queryUserGroup = Lists.newArrayList( );
  private String attribute;
  private String operationType;

  public ImageAttribute imageAttribute( ) {
    if ( !Strings.isNullOrEmpty( attribute ) ) {
      return "launchPermission".equals( attribute ) ? ImageAttribute.LaunchPermission : ImageAttribute.ProductCode;
    } else {
      return launchPermission != null && ( !launchPermission.getAdd( ).isEmpty( ) || !launchPermission.getRemove( ).isEmpty( ) ) ? ImageAttribute.LaunchPermission : !productCodes.isEmpty( ) ? ImageAttribute.ProductCode : ImageAttribute.Description;
    }

  }

  public boolean add( ) {
    return !asAddLaunchPermissionsItemTypes( ).isEmpty( );
  }

  public List<String> userIds( ) {
    List<String> userIds = Lists.newArrayList( );
    for ( LaunchPermissionItemType item : ( add( ) ? asAddLaunchPermissionsItemTypes( ) : asRemoveLaunchPermissionsItemTypes( ) ) ) {
      if ( item.getUserId( ) != null && !item.getUserId( ).isEmpty( ) ) {
        ( (ArrayList<String>) userIds ).add( item.getUserId( ) );
      }

    }

    return userIds;
  }

  public boolean groupAll( ) {
    boolean all = false;
    for ( LaunchPermissionItemType item : Stream.ofAll( asAddLaunchPermissionsItemTypes( ) ).appendAll( asRemoveLaunchPermissionsItemTypes( ) ) ) {
      if ( "all".equals( item.getGroup( ) ) ) {
        all = true;
        break;
      }

    }

    return all;
  }

  public List<LaunchPermissionItemType> asAddLaunchPermissionsItemTypes( ) {
    return !Strings.isNullOrEmpty( attribute ) ? "add".equals( operationType ) ? asLaunchPermissionItemTypes( ) : Lists.newArrayList( ) : launchPermission.getAdd( );
  }

  public List<LaunchPermissionItemType> asRemoveLaunchPermissionsItemTypes( ) {
    return !Strings.isNullOrEmpty( attribute ) ? "add".equals( operationType ) ? Lists.newArrayList( ) : asLaunchPermissionItemTypes( ) : launchPermission.getRemove( );
  }

  private List<LaunchPermissionItemType> asLaunchPermissionItemTypes( ) {
    return queryUserId.isEmpty( ) ? Stream.ofAll( queryUserGroup ).map( LaunchPermissionItemType.forGroup( ) ).toJavaList( ) : Stream.ofAll( queryUserId ).map( LaunchPermissionItemType.forUser( ) ).toJavaList( );
  }

  public String getImageId( ) {
    return imageId;
  }

  public void setImageId( String imageId ) {
    this.imageId = imageId;
  }

  public ArrayList<String> getProductCodes( ) {
    return productCodes;
  }

  public void setProductCodes( ArrayList<String> productCodes ) {
    this.productCodes = productCodes;
  }

  public LaunchPermissionOperationType getLaunchPermission( ) {
    return launchPermission;
  }

  public void setLaunchPermission( LaunchPermissionOperationType launchPermission ) {
    this.launchPermission = launchPermission;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
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

  public enum ImageAttribute {
    LaunchPermission, ProductCode, Description;
  }
}
