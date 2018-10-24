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
