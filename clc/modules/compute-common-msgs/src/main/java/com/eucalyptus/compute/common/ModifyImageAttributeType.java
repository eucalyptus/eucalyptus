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
        userIds.add( item.getUserId( ) );
      }

    }

    return userIds;
  }

  public boolean groupAll( ) {
    boolean all = false;
    for ( LaunchPermissionItemType item : StreamUtil.ofAll( asAddLaunchPermissionsItemTypes( ) ).appendAll( asRemoveLaunchPermissionsItemTypes( ) ) ) {
      if ( "all".equals( item.getGroup( ) ) ) {
        all = true;
        break;
      }

    }

    return all;
  }

  public List<LaunchPermissionItemType> asAddLaunchPermissionsItemTypes( ) {
    return !Strings.isNullOrEmpty( attribute ) ? "add".equals( operationType ) ? asLaunchPermissionItemTypes( ) : Lists.<LaunchPermissionItemType>newArrayList( ) : launchPermission.getAdd( );
  }

  public List<LaunchPermissionItemType> asRemoveLaunchPermissionsItemTypes( ) {
    return !Strings.isNullOrEmpty( attribute ) ? "add".equals( operationType ) ? Lists.<LaunchPermissionItemType>newArrayList( ) : asLaunchPermissionItemTypes( ) : launchPermission.getRemove( );
  }

  List<LaunchPermissionItemType> asLaunchPermissionItemTypes( ) {
    return queryUserId.isEmpty( ) ? StreamUtil.ofAll( queryUserGroup ).map( LaunchPermissionItemType.forGroup( ) ).toJavaList( ) : StreamUtil.ofAll( queryUserId ).map( LaunchPermissionItemType.forUser( ) ).toJavaList( );
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
    LaunchPermission, ProductCode, Description
  }
}
