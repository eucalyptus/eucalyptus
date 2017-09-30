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
import com.google.common.collect.Lists;


public class DescribeImageAttributeResponseType extends VmImageMessage {

  private String imageId;
  private ArrayList<LaunchPermissionItemType> launchPermission = Lists.newArrayList( );
  private ArrayList<String> productCodes = Lists.newArrayList( );
  private ArrayList<String> kernel = Lists.newArrayList( );
  private ArrayList<String> ramdisk = Lists.newArrayList( );
  private ArrayList<BlockDeviceMappingItemType> blockDeviceMapping = Lists.newArrayList( );
  private ArrayList<String> description = Lists.newArrayList( );

  public boolean hasLaunchPermissions( ) {
    return this.launchPermission != null && !this.launchPermission.isEmpty( );
  }

  public boolean hasBlockDeviceMapping( ) {
    return this.blockDeviceMapping != null && !this.blockDeviceMapping.isEmpty( );
  }

  public boolean hasProductCodes( ) {
    return this.productCodes != null && !this.productCodes.isEmpty( );
  }

  public boolean hasKernel( ) {
    return this.kernel != null && !this.kernel.isEmpty( );
  }

  public boolean hasRamdisk( ) {
    return this.ramdisk != null && !this.ramdisk.isEmpty( );
  }

  public boolean hasDescription( ) {
    return this.description != null && !this.description.isEmpty( );
  }

  public String getImageId( ) {
    return imageId;
  }

  public void setImageId( String imageId ) {
    this.imageId = imageId;
  }

  public ArrayList<LaunchPermissionItemType> getLaunchPermission( ) {
    return launchPermission;
  }

  public void setLaunchPermission( ArrayList<LaunchPermissionItemType> launchPermission ) {
    this.launchPermission = launchPermission;
  }

  public ArrayList<String> getProductCodes( ) {
    return productCodes;
  }

  public void setProductCodes( ArrayList<String> productCodes ) {
    this.productCodes = productCodes;
  }

  public ArrayList<String> getKernel( ) {
    return kernel;
  }

  public void setKernel( ArrayList<String> kernel ) {
    this.kernel = kernel;
  }

  public ArrayList<String> getRamdisk( ) {
    return ramdisk;
  }

  public void setRamdisk( ArrayList<String> ramdisk ) {
    this.ramdisk = ramdisk;
  }

  public ArrayList<BlockDeviceMappingItemType> getBlockDeviceMapping( ) {
    return blockDeviceMapping;
  }

  public void setBlockDeviceMapping( ArrayList<BlockDeviceMappingItemType> blockDeviceMapping ) {
    this.blockDeviceMapping = blockDeviceMapping;
  }

  public ArrayList<String> getDescription( ) {
    return description;
  }

  public void setDescription( ArrayList<String> description ) {
    this.description = description;
  }
}
