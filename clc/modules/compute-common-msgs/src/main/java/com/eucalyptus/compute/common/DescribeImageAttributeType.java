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

import com.eucalyptus.util.Beans;

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
    Beans.setObjectProperty( this, attribute, "hi" );
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
