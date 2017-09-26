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

import java.beans.PropertyDescriptor;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;

public class DescribeImageAttributeType extends VmImageMessage {

  private String imageId;
  private String launchPermission = "hi";
  private String productCodes = "hi";
  private String kernel = "hi";
  private String ramdisk = "hi";
  private String blockDeviceMapping = "hi";
  private String description = "hi";
  private String attribute;

  public void applyAttribute( ) {
    this.launchPermission = null;
    this.productCodes = null;
    this.kernel = null;
    this.ramdisk = null;
    this.blockDeviceMapping = null;
    this.description = null;
    PropertyDescriptor property = BeanUtils.getPropertyDescriptor( DescribeImageAttributeType.class, attribute );
    if ( property != null ) {
      ReflectionUtils.invokeMethod( property.getWriteMethod( ), this, "hi" );
    }

  }

  public String getImageId( ) {
    return imageId;
  }

  public void setImageId( String imageId ) {
    this.imageId = imageId;
  }

  public String getLaunchPermission( ) {
    return launchPermission;
  }

  public void setLaunchPermission( String launchPermission ) {
    this.launchPermission = launchPermission;
  }

  public String getProductCodes( ) {
    return productCodes;
  }

  public void setProductCodes( String productCodes ) {
    this.productCodes = productCodes;
  }

  public String getKernel( ) {
    return kernel;
  }

  public void setKernel( String kernel ) {
    this.kernel = kernel;
  }

  public String getRamdisk( ) {
    return ramdisk;
  }

  public void setRamdisk( String ramdisk ) {
    this.ramdisk = ramdisk;
  }

  public String getBlockDeviceMapping( ) {
    return blockDeviceMapping;
  }

  public void setBlockDeviceMapping( String blockDeviceMapping ) {
    this.blockDeviceMapping = blockDeviceMapping;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getAttribute( ) {
    return attribute;
  }

  public void setAttribute( String attribute ) {
    this.attribute = attribute;
  }
}
