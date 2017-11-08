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

import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.binding.HttpEmbedded
import com.google.common.base.Function
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

import com.google.common.collect.Lists;

public class VmImageMessage extends ComputeMessage {
  
  public VmImageMessage( ) {
    super( );
  }
  
  public VmImageMessage( ComputeMessage msg ) {
    super( msg );
  }
  
  public VmImageMessage( String userId ) {
    super( userId );
  }
}
/** *******************************************************************************/
public class DeregisterImageResponseType extends VmImageMessage {
}

public class DeregisterImageType extends VmImageMessage {
  
  String imageId;
}
/** *******************************************************************************/
public class DescribeImageAttributeResponseType extends VmImageMessage {
  String imageId
  ArrayList<LaunchPermissionItemType> launchPermission = Lists.newArrayList( )
  ArrayList<String> productCodes = Lists.newArrayList( )
  ArrayList<String> kernel = Lists.newArrayList( )
  ArrayList<String> ramdisk = Lists.newArrayList( )
  ArrayList<BlockDeviceMappingItemType> blockDeviceMapping = Lists.newArrayList( )
  ArrayList<String> description = Lists.newArrayList( )

  boolean hasLaunchPermissions() {
    this.launchPermission
  }
  boolean hasBlockDeviceMapping() {
    this.blockDeviceMapping
  }
  boolean hasProductCodes() {
    this.productCodes
  }
  boolean hasKernel() {
    this.kernel
  }
  boolean hasRamdisk() {
    this.ramdisk
  }
  boolean hasDescription() {
    this.description
  }
}

public class DescribeImageAttributeType extends VmImageMessage {
  
  String imageId;
  String launchPermission = "hi";
  String productCodes = "hi";
  String kernel = "hi";
  String ramdisk = "hi";
  String blockDeviceMapping = "hi";
  String description = "hi";
  String attribute;
  
  public void applyAttribute() {
    this.launchPermission = null;
    this.productCodes = null;
    this.kernel = null;
    this.ramdisk = null;
    this.blockDeviceMapping = null;
    this.description = null;
    this.setProperty(attribute, "hi");
  }
}
/** *******************************************************************************/
public class DescribeImagesResponseType extends VmImageMessage {
  
  ArrayList<ImageDetails> imagesSet = new ArrayList<ImageDetails>();
}

public class DescribeImagesType extends VmImageMessage {
  
  @HttpParameterMapping (parameter = "ExecutableBy")
  ArrayList<String> executableBySet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "ImageId")
  ArrayList<String> imagesSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Owner")
  ArrayList<String> ownersSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}
/** *******************************************************************************/
public class ModifyImageAttributeResponseType extends VmImageMessage {
}

public class ModifyImageAttributeType extends VmImageMessage {
  enum ImageAttribute { LaunchPermission, ProductCode, Description }

  String imageId;
  @HttpParameterMapping (parameter = "ProductCode")
  ArrayList<String> productCodes = Lists.newArrayList()

  // Post 2010-06-15
  LaunchPermissionOperationType launchPermission
  @HttpParameterMapping( parameter = "Description.Value")
  String description

  // Up to 2010-06-15
  @HttpParameterMapping( parameter = "UserId")
  ArrayList<String> queryUserId = Lists.newArrayList()
  @HttpParameterMapping (parameter = ["Group","UserGroup"])
  ArrayList<String> queryUserGroup = Lists.newArrayList()
  String attribute;
  String operationType;

  ImageAttribute imageAttribute( ) {
    if ( attribute ) {
      'launchPermission'.equals( attribute ) ?
        ImageAttribute.LaunchPermission :
        ImageAttribute.ProductCode
    } else {
      launchPermission && (!launchPermission.getAdd().isEmpty() || !launchPermission.getRemove().isEmpty()) ?
        ImageAttribute.LaunchPermission :
        !productCodes.isEmpty() ?
          ImageAttribute.ProductCode :
          ImageAttribute.Description
    }
  }

  boolean add() {
    !asAddLaunchPermissionsItemTypes().isEmpty()
  }

  List<String> userIds() {
    add() ?
      asAddLaunchPermissionsItemTypes().collect{ LaunchPermissionItemType add -> add.userId }.findAll{ String id -> id } as List<String> :
      asRemoveLaunchPermissionsItemTypes().collect{ LaunchPermissionItemType remove -> remove.userId }.findAll{ String id -> id } as List<String>
  }

  boolean groupAll() {
    ( asAddLaunchPermissionsItemTypes().find{ LaunchPermissionItemType add -> 'all'.equals( add.getGroup() ) } ||
      asRemoveLaunchPermissionsItemTypes().find{ LaunchPermissionItemType remove -> 'all'.equals( remove.getGroup() ) } )
  }

  List<LaunchPermissionItemType> asAddLaunchPermissionsItemTypes() {
    attribute ?
      'add'.equals( operationType ) ? asLaunchPermissionItemTypes() : Lists.newArrayList() :
      launchPermission.getAdd()
  }

  List<LaunchPermissionItemType> asRemoveLaunchPermissionsItemTypes() {
    attribute ?
      'add'.equals( operationType ) ? Lists.newArrayList() : asLaunchPermissionItemTypes() :
      launchPermission.getRemove()
  }

  private List<LaunchPermissionItemType> asLaunchPermissionItemTypes() {
    queryUserId.isEmpty() ?
      queryUserGroup.collect{ String group -> new LaunchPermissionItemType( group: group ) } :
      queryUserId.collect{ String userId -> new LaunchPermissionItemType( userId: userId ) }
  }

}
/** *******************************************************************************/
public class RegisterImageResponseType extends VmImageMessage {
  String imageId;
}

public class RegisterImageType extends VmImageMessage {
  String imageLocation;
  String amiId;
  String name;
  String description;
  String architecture;
  String kernelId;
  String ramdiskId;
  String rootDeviceName;
  String virtualizationType;
  String platform;
  String sriovNetSupport
  @HttpParameterMapping (parameter = "BlockDeviceMapping")
  @HttpEmbedded (multiple = true)
  ArrayList<BlockDeviceMappingItemType> blockDeviceMappings = new ArrayList<BlockDeviceMappingItemType>();
}

public class CopyImageType extends VmImageMessage {
  String sourceRegion
  String sourceImageId
  String name
  String description
  String clientToken
}

public class CopyImageResponseType extends VmImageMessage {
  String imageId
}

/** *******************************************************************************/
public class ResetImageAttributeResponseType extends VmImageMessage {
}

public class ResetImageAttributeType extends VmImageMessage {
  String imageId;
  @HttpParameterMapping (parameter = "Attribute")
  String launchPermission;
}
/** *******************************************************************************/
public class ImageDetails extends EucalyptusData {
  
  String imageId;
  String imageLocation;
  String imageState;
  String imageOwnerId;
  String architecture;
  String imageType;
  String kernelId;
  String ramdiskId;
  String platform;
  Boolean isPublic;
  String imageOwnerAlias;
  String rootDeviceType = "instance-store";
  String rootDeviceName = "/dev/sda1";
  String name;
  String description;
  String virtualizationType;
  String hypervisor;
  Date creationDate;
  ArrayList<BlockDeviceMappingItemType> blockDeviceMappings = new ArrayList<BlockDeviceMappingItemType>();
  ArrayList<String> productCodes = new ArrayList<String>();
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>();

  static Function<ImageDetails,String> imageId( ) {
    { ImageDetails details -> details.imageId } as Function<ImageDetails,String>
  }

  boolean equals(final Object o) {
    if ( !o == null || !(o instanceof ImageDetails) ) return false;
    ImageDetails that = (ImageDetails) o;
    if ( !imageId.equals(that.imageId) ) return false;
    return true;
  }
  
  int hashCode() {
    return imageId.hashCode();
  }
}

public class LaunchPermissionOperationType extends EucalyptusData {
  @HttpEmbedded( multiple = true )
  ArrayList<LaunchPermissionItemType> add = Lists.newArrayList()
  @HttpEmbedded( multiple = true )
  ArrayList<LaunchPermissionItemType> remove = Lists.newArrayList()

  def LaunchPermissionOperationType() {
  }
}

public class LaunchPermissionItemType extends EucalyptusData {

  String userId;
  String group;
  
  def LaunchPermissionItemType() {
  }
  
  def LaunchPermissionItemType(final String userId, final String group) {
    this.userId = userId;
    this.group = group;
  }
  
  public static LaunchPermissionItemType newUserLaunchPermission(String userId) {
    return new LaunchPermissionItemType(userId, null);
  }
  
  public static LaunchPermissionItemType newGroupLaunchPermission() {
    return new LaunchPermissionItemType(null, "all" );
  }
  
  public boolean user() {
    return this.userId != null
  }
  
  public boolean group() {
    return this.group != null
  }

  boolean equals(final o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    final LaunchPermissionItemType that = (LaunchPermissionItemType) o

    if (getGroup() != that.getGroup()) return false
    if (userId != that.userId) return false

    return true
  }

  int hashCode() {
    int result
    result = (userId != null ? userId.hashCode() : 0)
    result = 31 * result + (getGroup() != null ? getGroup().hashCode() : 0)
    return result
  }
}
public class ConfirmProductInstanceResponseType extends VmImageMessage {
  String ownerId;
}

public class ConfirmProductInstanceType extends VmImageMessage {
  String productCode;
  String instanceId;
}
public class ResolveVmImageInfo extends VmImageMessage {
  String imageId;
  String kernelId;
  String ramdiskId;
}
//TODO:ADDED
public class CreateImageResponseType extends VmImageMessage {
  String imageId;
  public CreateImageResponseType() {  }
}

public class CreateImageType extends VmImageMessage {
  String instanceId;
  String name;
  String description;
  Boolean noReboot;
  @HttpParameterMapping (parameter = "BlockDeviceMapping")
  @HttpEmbedded (multiple = true)
  ArrayList<BlockDeviceMappingItemType> blockDeviceMappings = new ArrayList<BlockDeviceMappingItemType>();
  public CreateImageType() {  }
}
