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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.msgs

import edu.ucsb.eucalyptus.annotation.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.BlockDeviceMappingItemType;

public class VmImageMessage extends EucalyptusMessage {}
/** *******************************************************************************/
public class DeregisterImageResponseType extends VmImageMessage {

  boolean _return;
}
public class DeregisterImageType extends VmImageMessage {

  String imageId;
}
/** *******************************************************************************/
public class DescribeImageAttributeResponseType extends VmImageMessage {

  String imageId;
  ArrayList<LaunchPermissionItemType> launchPermission = new ArrayList<LaunchPermissionItemType>();
  ArrayList<String> productCodes = new ArrayList<String>();
  ArrayList<String> kernel = new ArrayList<String>();
  ArrayList<String> ramdisk = new ArrayList<String>();
  ArrayList<BlockDeviceMappingItemType> blockDeviceMapping = new ArrayList<BlockDeviceMappingItemType>();
  protected ArrayList realResponse;

  public void setRealResponse( ArrayList r ) { this.realResponse = r; }
  public boolean hasLaunchPermissions() { return this.realResponse.is(this.launchPermission); }
  public boolean hasBlockDeviceMapping() { return this.realResponse.is(this.blockDeviceMapping); }
  public boolean hasProductCodes() { return this.realResponse.is(this.productCodes); }
  public boolean hasKernel() { return this.realResponse.is(this.kernel); }
  public boolean hasRamdisk() { return this.realResponse.is(this.ramdisk); }
}
public class DescribeImageAttributeType extends VmImageMessage {

  String imageId;
  String launchPermission = "hi";
  String productCodes = "hi";
  String kernel = "hi";
  String ramdisk = "hi";
  String blockDeviceMapping = "hi";
  String attribute;

  public void applyAttribute() {
    this.launchPermission = null;
    this.productCodes = null;
    this.kernel = null;
    this.ramdisk = null;
    this.blockDeviceMapping = null;
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
}
/** *******************************************************************************/
public class ModifyImageAttributeResponseType extends VmImageMessage {
  boolean _return;
}
public class ModifyImageAttributeType extends VmImageMessage {
  String imageId;
  String attribute;
  String operationType;
  ArrayList<LaunchPermissionItemType> add = new ArrayList<LaunchPermissionItemType>();
  ArrayList<LaunchPermissionItemType> remove = new ArrayList<LaunchPermissionItemType>();
  @HttpParameterMapping (parameter = "UserId")
  ArrayList<String> queryUserId = new ArrayList<String>();
  @HttpParameterMapping (parameter = "UserGroup")
  ArrayList<String> queryUserGroup = new ArrayList<String>();
  @HttpParameterMapping (parameter = "ProductCode")
  ArrayList<String> productCodes = new ArrayList<String>();

  public void applyAttribute() {
    ArrayList<LaunchPermissionItemType> modifyMe = (operationType.equals( "add" )) ? this.add : this.remove;
    if ( !this.queryUserId.isEmpty() ) {
      for ( String userName: queryUserId ) {
        modifyMe.add(LaunchPermissionItemType.getUser(userName));
      }
    }
    if ( !this.queryUserGroup.isEmpty() ) {
      for ( String groupName: queryUserGroup ) {
        modifyMe.add(LaunchPermissionItemType.getGroup(groupName));
      }
    }
  }
}
/** *******************************************************************************/
public class RegisterImageResponseType extends VmImageMessage {
  String imageId;
}
public class RegisterImageType extends VmImageMessage {
  String imageLocation;
  String amiId;
}
/** *******************************************************************************/
public class ResetImageAttributeResponseType extends VmImageMessage {
  boolean _return;
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
  ArrayList<String> productCodes = new ArrayList<String>();

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
public class LaunchPermissionItemType extends EucalyptusData {

  String userId;
  String group;

  def LaunchPermissionItemType() {
  }

  def LaunchPermissionItemType(final String userId, final String group) {
    this.userId = userId;
    this.group = group;
  }

  public static LaunchPermissionItemType getUser(String userId) {
    return new LaunchPermissionItemType(userId, null);
  }

  public static LaunchPermissionItemType getGroup(String groupId) {
    return new LaunchPermissionItemType(null, groupId);
  }

  public boolean isUser() { return this.userId != null }

  public boolean isGroup() { return this.group != null }
}
/** *******************************************************************************/
public class EucaRegisterImageType extends VmImageMessage {

  ImageDetails image;
}
public class EucaRegisterImageResponseType extends VmImageMessage {

  String imageId;
}

public class ConfirmProductInstanceResponseType extends EucalyptusMessage {
  boolean _return;
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
