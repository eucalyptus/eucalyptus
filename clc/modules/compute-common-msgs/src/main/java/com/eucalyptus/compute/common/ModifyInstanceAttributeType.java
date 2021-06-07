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

import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;

public class ModifyInstanceAttributeType extends VmControlMessage {

  private String instanceId;
  private AttributeValueType instanceType;
  private AttributeValueType kernel;
  private AttributeValueType ramdisk;
  private AttributeValueType userData;
  private AttributeBooleanValueType disableApiTermination;
  private AttributeValueType instanceInitiatedShutdownBehavior;
  @HttpEmbedded
  private InstanceBlockDeviceMappingSetType blockDeviceMappingSet;
  private AttributeBooleanValueType sourceDestCheck;
  @HttpEmbedded
  private GroupIdSetType groupIdSet;
  @HttpParameterMapping( parameter = { "EbsOptimized", "EbsOptimized.Value" } )
  private AttributeBooleanFlatValueType ebsOptimized;
  private AttributeBooleanValueType sriovNetSupport;

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public AttributeValueType getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( AttributeValueType instanceType ) {
    this.instanceType = instanceType;
  }

  public AttributeValueType getKernel( ) {
    return kernel;
  }

  public void setKernel( AttributeValueType kernel ) {
    this.kernel = kernel;
  }

  public AttributeValueType getRamdisk( ) {
    return ramdisk;
  }

  public void setRamdisk( AttributeValueType ramdisk ) {
    this.ramdisk = ramdisk;
  }

  public AttributeValueType getUserData( ) {
    return userData;
  }

  public void setUserData( AttributeValueType userData ) {
    this.userData = userData;
  }

  public AttributeBooleanValueType getDisableApiTermination( ) {
    return disableApiTermination;
  }

  public void setDisableApiTermination( AttributeBooleanValueType disableApiTermination ) {
    this.disableApiTermination = disableApiTermination;
  }

  public AttributeValueType getInstanceInitiatedShutdownBehavior( ) {
    return instanceInitiatedShutdownBehavior;
  }

  public void setInstanceInitiatedShutdownBehavior( AttributeValueType instanceInitiatedShutdownBehavior ) {
    this.instanceInitiatedShutdownBehavior = instanceInitiatedShutdownBehavior;
  }

  public InstanceBlockDeviceMappingSetType getBlockDeviceMappingSet( ) {
    return blockDeviceMappingSet;
  }

  public void setBlockDeviceMappingSet( InstanceBlockDeviceMappingSetType blockDeviceMappingSet ) {
    this.blockDeviceMappingSet = blockDeviceMappingSet;
  }

  public AttributeBooleanValueType getSourceDestCheck( ) {
    return sourceDestCheck;
  }

  public void setSourceDestCheck( AttributeBooleanValueType sourceDestCheck ) {
    this.sourceDestCheck = sourceDestCheck;
  }

  public GroupIdSetType getGroupIdSet( ) {
    return groupIdSet;
  }

  public void setGroupIdSet( GroupIdSetType groupIdSet ) {
    this.groupIdSet = groupIdSet;
  }

  public AttributeBooleanFlatValueType getEbsOptimized( ) {
    return ebsOptimized;
  }

  public void setEbsOptimized( AttributeBooleanFlatValueType ebsOptimized ) {
    this.ebsOptimized = ebsOptimized;
  }

  public AttributeBooleanValueType getSriovNetSupport( ) {
    return sriovNetSupport;
  }

  public void setSriovNetSupport( AttributeBooleanValueType sriovNetSupport ) {
    this.sriovNetSupport = sriovNetSupport;
  }
}
