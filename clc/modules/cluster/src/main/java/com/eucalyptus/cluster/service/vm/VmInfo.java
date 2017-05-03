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
 ************************************************************************/package com.eucalyptus.cluster.service.vm;

import java.util.concurrent.ConcurrentMap;
import com.google.common.collect.Maps;

/**
 *
 */
public final class VmInfo {
  private final String id;
  private final String uuid;
  private final String reservationId;
  private final int launchIndex;

  private final String instanceTypeName;
  private final int instanceTypeCores;
  private final int instanceTypeDisk;
  private final int instanceTypeMemory;

  private final String platform;
  private final String sshKeyValue;
  private final long launchtime;
  private final VmInterface primaryInterface;
  private final ConcurrentMap<Integer, VmInterface> secondaryInterfaceAttachments = Maps.newConcurrentMap( );

  private final String ownerId;
  private final String accountId;
  private final String serviceTag;

  private volatile String vpcId;
  private volatile String state;
  private volatile long stateTimestamp;

  private final ConcurrentMap<String, VmVolumeAttachment> volumeAttachments = Maps.newConcurrentMap( );

  public VmInfo(
      final String id,
      final String uuid,
      final String reservationId,
      final int launchIndex,
      final String instanceTypeName,
      final int instanceTypeCores,
      final int instanceTypeDisk,
      final int instanceTypeMemory,
      final String platform,
      final String sshKeyValue,
      final long launchtime,
      final String state,
      final long stateTimestamp,
      final String interfaceId,
      final String attachmentId,
      final String mac,
      final String privateAddress,
      final String publicAddress,
      final String ownerId,
      final String accountId,
      final String serviceTag,
      final String vpcId
  ) {
    this.id = id;
    this.uuid = uuid;
    this.reservationId = reservationId;
    this.launchIndex = launchIndex;
    this.instanceTypeName = instanceTypeName;
    this.instanceTypeCores = instanceTypeCores;
    this.instanceTypeDisk = instanceTypeDisk;
    this.instanceTypeMemory = instanceTypeMemory;
    this.platform = platform;
    this.sshKeyValue = sshKeyValue;
    this.launchtime = launchtime;
    this.state = state;
    this.stateTimestamp = stateTimestamp;
    this.primaryInterface = new VmInterface(
        interfaceId,
        attachmentId,
        0,
        mac,
        privateAddress,
        publicAddress
    );
    this.ownerId = ownerId;
    this.accountId = accountId;
    this.serviceTag = serviceTag;
    this.vpcId = vpcId;
  }

  public String getId() {
    return id;
  }

  public String getUuid() {
    return uuid;
  }

  public String getReservationId() {
    return reservationId;
  }

  public int getLaunchIndex() {
    return launchIndex;
  }

  public String getInstanceTypeName() {
    return instanceTypeName;
  }

  public int getInstanceTypeCores() {
    return instanceTypeCores;
  }

  public int getInstanceTypeDisk() {
    return instanceTypeDisk;
  }

  public int getInstanceTypeMemory() {
    return instanceTypeMemory;
  }

  public String getPlatform() {
    return platform;
  }

  public String getSshKeyValue() {
    return sshKeyValue;
  }

  public long getLaunchtime() {
    return launchtime;
  }

  public VmInterface getPrimaryInterface() {
    return primaryInterface;
  }

  public ConcurrentMap<Integer, VmInterface> getSecondaryInterfaceAttachments() {
    return secondaryInterfaceAttachments;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public String getAccountId() {
    return accountId;
  }

  public String getServiceTag() {
    return serviceTag;
  }

  public String getVpcId() {
    return vpcId;
  }

  public void setVpcId( final String vpcId ) {
    this.vpcId = vpcId;
  }

  public String getState() {
    return state;
  }

  public void setState( final String state ) {
    this.state = state;
  }

  public long getStateTimestamp() {
    return stateTimestamp;
  }

  public void setStateTimestamp( final long stateTimestamp ) {
    this.stateTimestamp = stateTimestamp;
  }

  public ConcurrentMap<String, VmVolumeAttachment> getVolumeAttachments() {
    return volumeAttachments;
  }
}
