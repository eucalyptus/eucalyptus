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
package com.eucalyptus.cluster.common.msgs;

import java.util.ArrayList;
import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class VmInfo extends EucalyptusData {

  private String uuid;
  private String imageId;
  private String kernelId;
  private String ramdiskId;
  private String instanceId;
  private VmTypeInfo instanceType = new VmTypeInfo( );
  private String keyValue;
  private Date launchTime;
  private String stateName;
  private NetworkConfigType netParams = new NetworkConfigType( );
  private String ownerId;
  private String accountId;
  private String reservationId;
  private String serviceTag;
  private String userData;
  private String launchIndex;
  private Long networkBytes = 0l;
  private Long blockBytes = 0l;
  private ArrayList<String> groupNames = new ArrayList<String>( );
  private ArrayList<AttachedVolume> volumes = new ArrayList<AttachedVolume>( );
  private String placement;
  private String platform;
  private String bundleTaskStateName;
  private Double bundleTaskProgress;
  private String createImageStateName;
  private String guestStateName;
  private String migrationStateName;
  private String migrationSource;
  private String migrationDestination;
  private ArrayList<NetworkConfigType> secondaryNetConfigList = new ArrayList<>( );
  private ArrayList<String> productCodes = new ArrayList<String>( );

  @Override
  public String toString( ) {
    return "VmInfo " + reservationId + " " + instanceId + " " + ownerId + " " + stateName + " " + String.valueOf( instanceType ) + " " + imageId + " " + kernelId + " " + ramdiskId + " " + launchIndex + " " + serviceTag + " " + String.valueOf( netParams ) + " " + String.valueOf( volumes ) + " " + migrationStateName + " " + String.valueOf( secondaryNetConfigList );
  }

  public String getUuid( ) {
    return uuid;
  }

  public void setUuid( String uuid ) {
    this.uuid = uuid;
  }

  public String getImageId( ) {
    return imageId;
  }

  public void setImageId( String imageId ) {
    this.imageId = imageId;
  }

  public String getKernelId( ) {
    return kernelId;
  }

  public void setKernelId( String kernelId ) {
    this.kernelId = kernelId;
  }

  public String getRamdiskId( ) {
    return ramdiskId;
  }

  public void setRamdiskId( String ramdiskId ) {
    this.ramdiskId = ramdiskId;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public VmTypeInfo getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( VmTypeInfo instanceType ) {
    this.instanceType = instanceType;
  }

  public String getKeyValue( ) {
    return keyValue;
  }

  public void setKeyValue( String keyValue ) {
    this.keyValue = keyValue;
  }

  public Date getLaunchTime( ) {
    return launchTime;
  }

  public void setLaunchTime( Date launchTime ) {
    this.launchTime = launchTime;
  }

  public String getStateName( ) {
    return stateName;
  }

  public void setStateName( String stateName ) {
    this.stateName = stateName;
  }

  public NetworkConfigType getNetParams( ) {
    return netParams;
  }

  public void setNetParams( NetworkConfigType netParams ) {
    this.netParams = netParams;
  }

  public String getOwnerId( ) {
    return ownerId;
  }

  public void setOwnerId( String ownerId ) {
    this.ownerId = ownerId;
  }

  public String getAccountId( ) {
    return accountId;
  }

  public void setAccountId( String accountId ) {
    this.accountId = accountId;
  }

  public String getReservationId( ) {
    return reservationId;
  }

  public void setReservationId( String reservationId ) {
    this.reservationId = reservationId;
  }

  public String getServiceTag( ) {
    return serviceTag;
  }

  public void setServiceTag( String serviceTag ) {
    this.serviceTag = serviceTag;
  }

  public String getUserData( ) {
    return userData;
  }

  public void setUserData( String userData ) {
    this.userData = userData;
  }

  public String getLaunchIndex( ) {
    return launchIndex;
  }

  public void setLaunchIndex( String launchIndex ) {
    this.launchIndex = launchIndex;
  }

  public Long getNetworkBytes( ) {
    return networkBytes;
  }

  public void setNetworkBytes( Long networkBytes ) {
    this.networkBytes = networkBytes;
  }

  public Long getBlockBytes( ) {
    return blockBytes;
  }

  public void setBlockBytes( Long blockBytes ) {
    this.blockBytes = blockBytes;
  }

  public ArrayList<String> getGroupNames( ) {
    return groupNames;
  }

  public void setGroupNames( ArrayList<String> groupNames ) {
    this.groupNames = groupNames;
  }

  public ArrayList<AttachedVolume> getVolumes( ) {
    return volumes;
  }

  public void setVolumes( ArrayList<AttachedVolume> volumes ) {
    this.volumes = volumes;
  }

  public String getPlacement( ) {
    return placement;
  }

  public void setPlacement( String placement ) {
    this.placement = placement;
  }

  public String getPlatform( ) {
    return platform;
  }

  public void setPlatform( String platform ) {
    this.platform = platform;
  }

  public String getBundleTaskStateName( ) {
    return bundleTaskStateName;
  }

  public void setBundleTaskStateName( String bundleTaskStateName ) {
    this.bundleTaskStateName = bundleTaskStateName;
  }

  public Double getBundleTaskProgress( ) {
    return bundleTaskProgress;
  }

  public void setBundleTaskProgress( Double bundleTaskProgress ) {
    this.bundleTaskProgress = bundleTaskProgress;
  }

  public String getCreateImageStateName( ) {
    return createImageStateName;
  }

  public void setCreateImageStateName( String createImageStateName ) {
    this.createImageStateName = createImageStateName;
  }

  public String getGuestStateName( ) {
    return guestStateName;
  }

  public void setGuestStateName( String guestStateName ) {
    this.guestStateName = guestStateName;
  }

  public String getMigrationStateName( ) {
    return migrationStateName;
  }

  public void setMigrationStateName( String migrationStateName ) {
    this.migrationStateName = migrationStateName;
  }

  public String getMigrationSource( ) {
    return migrationSource;
  }

  public void setMigrationSource( String migrationSource ) {
    this.migrationSource = migrationSource;
  }

  public String getMigrationDestination( ) {
    return migrationDestination;
  }

  public void setMigrationDestination( String migrationDestination ) {
    this.migrationDestination = migrationDestination;
  }

  public ArrayList<NetworkConfigType> getSecondaryNetConfigList( ) {
    return secondaryNetConfigList;
  }

  public void setSecondaryNetConfigList( ArrayList<NetworkConfigType> secondaryNetConfigList ) {
    this.secondaryNetConfigList = secondaryNetConfigList;
  }

  public ArrayList<String> getProductCodes( ) {
    return productCodes;
  }

  public void setProductCodes( ArrayList<String> productCodes ) {
    this.productCodes = productCodes;
  }
}
