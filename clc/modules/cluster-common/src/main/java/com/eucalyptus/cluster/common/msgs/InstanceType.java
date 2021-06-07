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

public class InstanceType extends EucalyptusData {

  private String uuid;
  private String reservationId;
  private String instanceId;
  private String imageId;
  private String kernelId;
  private String ramdiskId;
  private String userId;
  private String ownerId;
  private String accountId;
  private String keyName;
  private VirtualMachineType instanceType;
  private NetConfigType netParams;
  private String stateName;
  private String bundleTaskStateName;
  private Double bundleTaskProgress;
  private String createImageStateName;
  private Date launchTime;
  private Date expiryTime;
  private Integer blkbytes;
  private Integer netbytes;
  private String guestStateName;
  private String migrationStateName;
  private String migrationSource;
  private String migrationDestination;
  private String userData;
  private String launchIndex;
  private String platform;
  private String serviceTag;
  private Integer hasFloopy;
  private ArrayList<String> groupNames = new ArrayList<String>( );
  private ArrayList<String> groupIds = new ArrayList<String>( );
  private ArrayList<VolumeType> volumes = new ArrayList<VolumeType>( );
  private ArrayList<NetConfigType> secondaryNetConfig = new ArrayList<NetConfigType>( );

  public String getUuid( ) {
    return uuid;
  }

  public void setUuid( String uuid ) {
    this.uuid = uuid;
  }

  public String getReservationId( ) {
    return reservationId;
  }

  public void setReservationId( String reservationId ) {
    this.reservationId = reservationId;
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

  public String getUserId( ) {
    return userId;
  }

  public void setUserId( String userId ) {
    this.userId = userId;
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

  public String getKeyName( ) {
    return keyName;
  }

  public void setKeyName( String keyName ) {
    this.keyName = keyName;
  }

  public VirtualMachineType getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( VirtualMachineType instanceType ) {
    this.instanceType = instanceType;
  }

  public NetConfigType getNetParams( ) {
    return netParams;
  }

  public void setNetParams( NetConfigType netParams ) {
    this.netParams = netParams;
  }

  public String getStateName( ) {
    return stateName;
  }

  public void setStateName( String stateName ) {
    this.stateName = stateName;
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

  public Date getLaunchTime( ) {
    return launchTime;
  }

  public void setLaunchTime( Date launchTime ) {
    this.launchTime = launchTime;
  }

  public Date getExpiryTime( ) {
    return expiryTime;
  }

  public void setExpiryTime( Date expiryTime ) {
    this.expiryTime = expiryTime;
  }

  public Integer getBlkbytes( ) {
    return blkbytes;
  }

  public void setBlkbytes( Integer blkbytes ) {
    this.blkbytes = blkbytes;
  }

  public Integer getNetbytes( ) {
    return netbytes;
  }

  public void setNetbytes( Integer netbytes ) {
    this.netbytes = netbytes;
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

  public String getPlatform( ) {
    return platform;
  }

  public void setPlatform( String platform ) {
    this.platform = platform;
  }

  public String getServiceTag( ) {
    return serviceTag;
  }

  public void setServiceTag( String serviceTag ) {
    this.serviceTag = serviceTag;
  }

  public Integer getHasFloopy( ) {
    return hasFloopy;
  }

  public void setHasFloopy( Integer hasFloopy ) {
    this.hasFloopy = hasFloopy;
  }

  public ArrayList<String> getGroupNames( ) {
    return groupNames;
  }

  public void setGroupNames( ArrayList<String> groupNames ) {
    this.groupNames = groupNames;
  }

  public ArrayList<String> getGroupIds( ) {
    return groupIds;
  }

  public void setGroupIds( ArrayList<String> groupIds ) {
    this.groupIds = groupIds;
  }

  public ArrayList<VolumeType> getVolumes( ) {
    return volumes;
  }

  public void setVolumes( ArrayList<VolumeType> volumes ) {
    this.volumes = volumes;
  }

  public ArrayList<NetConfigType> getSecondaryNetConfig( ) {
    return secondaryNetConfig;
  }

  public void setSecondaryNetConfig( ArrayList<NetConfigType> secondaryNetConfig ) {
    this.secondaryNetConfig = secondaryNetConfig;
  }
}
