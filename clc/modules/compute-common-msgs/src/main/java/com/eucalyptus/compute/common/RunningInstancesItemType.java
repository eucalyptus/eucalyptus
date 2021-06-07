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
import java.util.Date;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class RunningInstancesItemType extends EucalyptusData implements Comparable<RunningInstancesItemType> {

  private String instanceId;
  private String imageId;
  private String stateCode;
  private String stateName;
  private String privateDnsName;
  private String dnsName;
  private String reason;
  private String keyName;
  private String amiLaunchIndex;
  private ArrayList<String> productCodes = new ArrayList<String>( );
  private String instanceType;
  private Date launchTime;
  private String placement;
  private String kernel;
  private String ramdisk;
  private String platform;
  private String architecture;
  private String monitoring;
  private Boolean disableApiTermination = false;
  private String instanceInitiatedShutdownBehavior = "stop";
  private String ipAddress;
  private String privateIpAddress;
  private String rootDeviceType = "instance-store";
  private String rootDeviceName = "/dev/sda1";
  private ArrayList<InstanceBlockDeviceMapping> blockDevices = new ArrayList<InstanceBlockDeviceMapping>( );
  private String virtualizationType;
  private String clientToken;
  private IamInstanceProfile iamInstanceProfile = new IamInstanceProfile( );
  private ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>( );
  private ArrayList<GroupItemType> groupSet = Lists.newArrayList( );
  private String subnetId;
  private String vpcId;
  private Boolean sourceDestCheck;
  private InstanceNetworkInterfaceSetType networkInterfaceSet;

  @Override
  public int compareTo( RunningInstancesItemType that ) {
    return this.instanceId.compareTo( that.instanceId );
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.instanceId == null ) ? 0 : this.instanceId.hashCode( ) );
    return result;
  }

  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }

    if ( obj == null ) {
      return false;
    }

    if ( !getClass( ).equals( obj.getClass( ) ) ) {
      return false;
    }

    RunningInstancesItemType other = (RunningInstancesItemType) obj;
    if ( this.instanceId == null ) {
      if ( other.instanceId != null ) {
        return false;
      }

    } else if ( !this.instanceId.equals( other.instanceId ) ) {
      return false;
    }

    return true;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getImageId( ) {
    return imageId;
  }

  public void setImageId( String imageId ) {
    this.imageId = imageId;
  }

  public String getStateCode( ) {
    return stateCode;
  }

  public void setStateCode( String stateCode ) {
    this.stateCode = stateCode;
  }

  public String getStateName( ) {
    return stateName;
  }

  public void setStateName( String stateName ) {
    this.stateName = stateName;
  }

  public String getPrivateDnsName( ) {
    return privateDnsName;
  }

  public void setPrivateDnsName( String privateDnsName ) {
    this.privateDnsName = privateDnsName;
  }

  public String getDnsName( ) {
    return dnsName;
  }

  public void setDnsName( String dnsName ) {
    this.dnsName = dnsName;
  }

  public String getReason( ) {
    return reason;
  }

  public void setReason( String reason ) {
    this.reason = reason;
  }

  public String getKeyName( ) {
    return keyName;
  }

  public void setKeyName( String keyName ) {
    this.keyName = keyName;
  }

  public String getAmiLaunchIndex( ) {
    return amiLaunchIndex;
  }

  public void setAmiLaunchIndex( String amiLaunchIndex ) {
    this.amiLaunchIndex = amiLaunchIndex;
  }

  public ArrayList<String> getProductCodes( ) {
    return productCodes;
  }

  public void setProductCodes( ArrayList<String> productCodes ) {
    this.productCodes = productCodes;
  }

  public String getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( String instanceType ) {
    this.instanceType = instanceType;
  }

  public Date getLaunchTime( ) {
    return launchTime;
  }

  public void setLaunchTime( Date launchTime ) {
    this.launchTime = launchTime;
  }

  public String getPlacement( ) {
    return placement;
  }

  public void setPlacement( String placement ) {
    this.placement = placement;
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

  public String getPlatform( ) {
    return platform;
  }

  public void setPlatform( String platform ) {
    this.platform = platform;
  }

  public String getArchitecture( ) {
    return architecture;
  }

  public void setArchitecture( String architecture ) {
    this.architecture = architecture;
  }

  public String getMonitoring( ) {
    return monitoring;
  }

  public void setMonitoring( String monitoring ) {
    this.monitoring = monitoring;
  }

  public Boolean getDisableApiTermination( ) {
    return disableApiTermination;
  }

  public void setDisableApiTermination( Boolean disableApiTermination ) {
    this.disableApiTermination = disableApiTermination;
  }

  public String getInstanceInitiatedShutdownBehavior( ) {
    return instanceInitiatedShutdownBehavior;
  }

  public void setInstanceInitiatedShutdownBehavior( String instanceInitiatedShutdownBehavior ) {
    this.instanceInitiatedShutdownBehavior = instanceInitiatedShutdownBehavior;
  }

  public String getIpAddress( ) {
    return ipAddress;
  }

  public void setIpAddress( String ipAddress ) {
    this.ipAddress = ipAddress;
  }

  public String getPrivateIpAddress( ) {
    return privateIpAddress;
  }

  public void setPrivateIpAddress( String privateIpAddress ) {
    this.privateIpAddress = privateIpAddress;
  }

  public String getRootDeviceType( ) {
    return rootDeviceType;
  }

  public void setRootDeviceType( String rootDeviceType ) {
    this.rootDeviceType = rootDeviceType;
  }

  public String getRootDeviceName( ) {
    return rootDeviceName;
  }

  public void setRootDeviceName( String rootDeviceName ) {
    this.rootDeviceName = rootDeviceName;
  }

  public ArrayList<InstanceBlockDeviceMapping> getBlockDevices( ) {
    return blockDevices;
  }

  public void setBlockDevices( ArrayList<InstanceBlockDeviceMapping> blockDevices ) {
    this.blockDevices = blockDevices;
  }

  public String getVirtualizationType( ) {
    return virtualizationType;
  }

  public void setVirtualizationType( String virtualizationType ) {
    this.virtualizationType = virtualizationType;
  }

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( String clientToken ) {
    this.clientToken = clientToken;
  }

  public IamInstanceProfile getIamInstanceProfile( ) {
    return iamInstanceProfile;
  }

  public void setIamInstanceProfile( IamInstanceProfile iamInstanceProfile ) {
    this.iamInstanceProfile = iamInstanceProfile;
  }

  public ArrayList<ResourceTag> getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ArrayList<ResourceTag> tagSet ) {
    this.tagSet = tagSet;
  }

  public ArrayList<GroupItemType> getGroupSet( ) {
    return groupSet;
  }

  public void setGroupSet( ArrayList<GroupItemType> groupSet ) {
    this.groupSet = groupSet;
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( String subnetId ) {
    this.subnetId = subnetId;
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
  }

  public Boolean getSourceDestCheck( ) {
    return sourceDestCheck;
  }

  public void setSourceDestCheck( Boolean sourceDestCheck ) {
    this.sourceDestCheck = sourceDestCheck;
  }

  public InstanceNetworkInterfaceSetType getNetworkInterfaceSet( ) {
    return networkInterfaceSet;
  }

  public void setNetworkInterfaceSet( InstanceNetworkInterfaceSetType networkInterfaceSet ) {
    this.networkInterfaceSet = networkInterfaceSet;
  }
}
