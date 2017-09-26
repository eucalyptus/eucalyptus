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
