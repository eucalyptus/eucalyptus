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
package com.eucalyptus.cluster.common.msgs;

import java.util.ArrayList;
import java.util.Date;

public class NcRunInstanceType extends CloudNodeMessage {

  private String imageId;
  private String kernelId;
  private String ramdiskId;
  private String imageURL;
  private String kernelURL;
  private String ramdiskURL;
  private String ownerId;
  private String accountId;
  private String reservationId;
  private String instanceId;
  private String uuid;
  private VirtualMachineType instanceType;
  private String keyName;
  private NetConfigType netParams;
  private String userData;
  private String credential;
  private String launchIndex;
  private String platform;
  private Date expiryTime;
  private String rootDirective;
  private ArrayList<String> groupNames = new ArrayList<String>( );
  private ArrayList<String> groupIds = new ArrayList<String>( );
  private ArrayList<NetConfigType> secondaryNetConfig = new ArrayList<NetConfigType>( );

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

  public String getImageURL( ) {
    return imageURL;
  }

  public void setImageURL( String imageURL ) {
    this.imageURL = imageURL;
  }

  public String getKernelURL( ) {
    return kernelURL;
  }

  public void setKernelURL( String kernelURL ) {
    this.kernelURL = kernelURL;
  }

  public String getRamdiskURL( ) {
    return ramdiskURL;
  }

  public void setRamdiskURL( String ramdiskURL ) {
    this.ramdiskURL = ramdiskURL;
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

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getUuid( ) {
    return uuid;
  }

  public void setUuid( String uuid ) {
    this.uuid = uuid;
  }

  public VirtualMachineType getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( VirtualMachineType instanceType ) {
    this.instanceType = instanceType;
  }

  public String getKeyName( ) {
    return keyName;
  }

  public void setKeyName( String keyName ) {
    this.keyName = keyName;
  }

  public NetConfigType getNetParams( ) {
    return netParams;
  }

  public void setNetParams( NetConfigType netParams ) {
    this.netParams = netParams;
  }

  public String getUserData( ) {
    return userData;
  }

  public void setUserData( String userData ) {
    this.userData = userData;
  }

  public String getCredential( ) {
    return credential;
  }

  public void setCredential( String credential ) {
    this.credential = credential;
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

  public Date getExpiryTime( ) {
    return expiryTime;
  }

  public void setExpiryTime( Date expiryTime ) {
    this.expiryTime = expiryTime;
  }

  public String getRootDirective( ) {
    return rootDirective;
  }

  public void setRootDirective( String rootDirective ) {
    this.rootDirective = rootDirective;
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

  public ArrayList<NetConfigType> getSecondaryNetConfig( ) {
    return secondaryNetConfig;
  }

  public void setSecondaryNetConfig( ArrayList<NetConfigType> secondaryNetConfig ) {
    this.secondaryNetConfig = secondaryNetConfig;
  }
}
