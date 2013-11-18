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
package edu.ucsb.eucalyptus.msgs;

import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.binding.HttpEmbedded;

public class VmImageMessage extends EucalyptusMessage {
  
  public VmImageMessage( ) {
    super( );
  }
  
  public VmImageMessage( EucalyptusMessage msg ) {
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
  ArrayList<LaunchPermissionItemType> launchPermission = []
  ArrayList<String> productCodes = []
  ArrayList<String> kernel = []
  ArrayList<String> ramdisk = []
  ArrayList<BlockDeviceMappingItemType> blockDeviceMapping = []
  ArrayList<String> description = []
  protected ArrayList realResponse
  
  public void setRealResponse( ArrayList r ) {
    this.realResponse = r;
  }
  public boolean hasLaunchPermissions() {
    return this.realResponse.is(this.launchPermission);
  }
  public boolean hasBlockDeviceMapping() {
    return this.realResponse.is(this.blockDeviceMapping);
  }
  public boolean hasProductCodes() {
    return this.realResponse.is(this.productCodes);
  }
  public boolean hasKernel() {
    return this.realResponse.is(this.kernel);
  }
  public boolean hasRamdisk() {
    return this.realResponse.is(this.ramdisk);
  }
  public boolean hasDescription() {
    return this.realResponse.is(this.description);
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
  ArrayList<String> productCodes = []

  // Post 2010-06-15
  @HttpEmbedded
  LaunchPermissionOperationType launchPermission
  @HttpParameterMapping( parameter = "Description.Value")
  String description

  // Up to 2010-06-15
  @HttpParameterMapping( parameter = "UserId")
  ArrayList<String> queryUserId = []
  @HttpParameterMapping (parameter = ["Group","UserGroup"])
  ArrayList<String> queryUserGroup = []
  String attribute;
  String operationType;

  ImageAttribute getImageAttribute( ) {
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

  boolean isAdd() {
    !getAdd().isEmpty()
  }

  List<String> getUserIds() {
    isAdd() ?
      getAdd().collect{ it.userId }.findAll{ it } :
      getRemove().collect{ it.userId }.findAll{ it }
  }

  boolean isGroupAll() {
    ( getAdd().find{ 'all'.equals( it.getGroup() ) } ||
      getRemove().find{ 'all'.equals( it.getGroup() ) } )
  }

  List<LaunchPermissionItemType> getAdd() {
    attribute ?
      'add'.equals( operationType ) ? asLaunchPermissionItemTypes() : [] :
      launchPermission.getAdd()
  }

  List<LaunchPermissionItemType> getRemove() {
    attribute ?
      'add'.equals( operationType ) ? [] : asLaunchPermissionItemTypes() :
      launchPermission.getRemove()
  }

  private List<LaunchPermissionItemType> asLaunchPermissionItemTypes() {
    queryUserId.isEmpty() ?
      queryUserGroup.collect{ new LaunchPermissionItemType( group: it ) } :
      queryUserId.collect{ new LaunchPermissionItemType( userId: it ) }
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
  String virtualizationType
  @HttpParameterMapping (parameter = "BlockDeviceMapping")
  @HttpEmbedded (multiple = true)
  ArrayList<BlockDeviceMappingItemType> blockDeviceMappings = new ArrayList<BlockDeviceMappingItemType>();
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
  ArrayList<BlockDeviceMappingItemType> blockDeviceMappings = new ArrayList<BlockDeviceMappingItemType>();
  ArrayList<String> productCodes = new ArrayList<String>();
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>();
  
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
  ArrayList<LaunchPermissionItemType> add = []
  @HttpEmbedded( multiple = true )
  ArrayList<LaunchPermissionItemType> remove = []

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
  
  public boolean isUser() {
    return this.userId != null
  }
  
  public boolean isGroup() {
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
