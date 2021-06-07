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

public class DescribeInstanceAttributeResponseType extends VmControlMessage {

  private String instanceId;
  private ArrayList<InstanceBlockDeviceMapping> blockDeviceMapping = Lists.newArrayList( );
  private Boolean disableApiTermination;
  private Boolean ebsOptimized;
  private ArrayList<GroupItemType> groupSet = Lists.newArrayList( );
  private String instanceInitiatedShutdownBehavior;
  private String instanceType;
  private String kernel;
  private Boolean productCodes;
  private String ramdisk;
  private String rootDeviceName;
  private Boolean sourceDestCheck;
  private Boolean sriovNetSupport;
  private String userData;

  public boolean hasDisableApiTermination( ) {
    return this.disableApiTermination != null;
  }

  public boolean hasEbsOptimized( ) {
    return this.ebsOptimized != null;
  }

  public boolean hasInstanceType( ) {
    return this.instanceType != null;
  }

  public boolean hasInstanceInitiatedShutdownBehavior( ) {
    return this.instanceInitiatedShutdownBehavior != null;
  }

  public boolean hasKernel( ) {
    return this.kernel != null;
  }

  public boolean hasProductCodes( ) {
    return this.productCodes != null;
  }

  public boolean hasRamdisk( ) {
    return this.ramdisk != null;
  }

  public boolean hasRootDeviceName( ) {
    return this.rootDeviceName != null;
  }

  public boolean hasUserData( ) {
    return this.userData != null;
  }

  public boolean hasNonEmptyUserData( ) {
    return this.userData != null && !this.userData.isEmpty( );
  }

  public boolean hasBlockDeviceMapping( ) {
    return !this.blockDeviceMapping.isEmpty( );
  }

  public boolean hasGroupSet( ) {
    return !this.groupSet.isEmpty( );
  }

  public boolean hasSourceDestCheck( ) {
    return this.sourceDestCheck != null;
  }

  public boolean hasSriovNetSupport( ) {
    return this.sriovNetSupport != null;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public ArrayList<InstanceBlockDeviceMapping> getBlockDeviceMapping( ) {
    return blockDeviceMapping;
  }

  public void setBlockDeviceMapping( ArrayList<InstanceBlockDeviceMapping> blockDeviceMapping ) {
    this.blockDeviceMapping = blockDeviceMapping;
  }

  public Boolean getDisableApiTermination( ) {
    return disableApiTermination;
  }

  public void setDisableApiTermination( Boolean disableApiTermination ) {
    this.disableApiTermination = disableApiTermination;
  }

  public Boolean getEbsOptimized( ) {
    return ebsOptimized;
  }

  public void setEbsOptimized( Boolean ebsOptimized ) {
    this.ebsOptimized = ebsOptimized;
  }

  public ArrayList<GroupItemType> getGroupSet( ) {
    return groupSet;
  }

  public void setGroupSet( ArrayList<GroupItemType> groupSet ) {
    this.groupSet = groupSet;
  }

  public String getInstanceInitiatedShutdownBehavior( ) {
    return instanceInitiatedShutdownBehavior;
  }

  public void setInstanceInitiatedShutdownBehavior( String instanceInitiatedShutdownBehavior ) {
    this.instanceInitiatedShutdownBehavior = instanceInitiatedShutdownBehavior;
  }

  public String getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( String instanceType ) {
    this.instanceType = instanceType;
  }

  public String getKernel( ) {
    return kernel;
  }

  public void setKernel( String kernel ) {
    this.kernel = kernel;
  }

  public Boolean getProductCodes( ) {
    return productCodes;
  }

  public void setProductCodes( Boolean productCodes ) {
    this.productCodes = productCodes;
  }

  public String getRamdisk( ) {
    return ramdisk;
  }

  public void setRamdisk( String ramdisk ) {
    this.ramdisk = ramdisk;
  }

  public String getRootDeviceName( ) {
    return rootDeviceName;
  }

  public void setRootDeviceName( String rootDeviceName ) {
    this.rootDeviceName = rootDeviceName;
  }

  public Boolean getSourceDestCheck( ) {
    return sourceDestCheck;
  }

  public void setSourceDestCheck( Boolean sourceDestCheck ) {
    this.sourceDestCheck = sourceDestCheck;
  }

  public Boolean getSriovNetSupport( ) {
    return sriovNetSupport;
  }

  public void setSriovNetSupport( Boolean sriovNetSupport ) {
    this.sriovNetSupport = sriovNetSupport;
  }

  public String getUserData( ) {
    return userData;
  }

  public void setUserData( String userData ) {
    this.userData = userData;
  }
}
