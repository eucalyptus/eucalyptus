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
package com.eucalyptus.cluster.service.vm;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentMap;
import com.eucalyptus.cluster.common.msgs.InstanceType;
import com.eucalyptus.cluster.common.msgs.MetricsResourceType;
import com.eucalyptus.cluster.common.msgs.VmRunType;
import com.eucalyptus.cluster.common.msgs.VolumeType;
import com.eucalyptus.crypto.util.Timestamps;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
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

  private volatile String vpcId;
  private volatile String state;
  private volatile long stateTimestamp;

  private final ConcurrentMap<String, VmVolumeAttachment> volumeAttachments = Maps.newConcurrentMap( );

  private volatile ArrayList<MetricsResourceType> metrics = new ArrayList<>( );

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
    this.vpcId = vpcId;
  }

  public static VmInfo create( final VmRunType request, final long currentTime ) {
    final VmInfo vmInfo = new VmInfo(
        request.getInstanceId( ),
        request.getUuid( ),
        request.getReservationId( ),
        request.getLaunchIndex( ),
        request.getVmTypeInfo( ).getName( ),
        request.getVmTypeInfo( ).getCores( ),
        request.getVmTypeInfo( ).getDisk( ),
        request.getVmTypeInfo( ).getMemory( ),
        request.getPlatform( ),
        request.getKeyInfo( ).getValue( ),
        currentTime,
        "Extant",
        currentTime,
        null,
        request.getPrimaryEniAttachmentId( ),
        request.getMacAddress( ),
        request.getPrivateAddress( ),
        null,
        request.getOwnerId( ),
        request.getAccountId( ),
        null
    );
    request.getVmTypeInfo( ).getVirtualBootRecord( ).forEach( vbr -> {
      if ( Strings.nullToEmpty( vbr.getId( ) ).startsWith( "vol-" ) ) {
        vmInfo.getVolumeAttachments( ).put( vbr.getId( ), new VmVolumeAttachment(
            System.currentTimeMillis( ),
            vbr.getId( ),
            vbr.getGuestDeviceName( ),
            vbr.getResourceLocation( ),
            "attached"
        ) );
      }
    } );
    return vmInfo;
  }

  public static VmInfo create( final InstanceType instance ) {
    final VmInfo vmInfo = new VmInfo(
        instance.getInstanceId( ),
        instance.getUuid( ),
        instance.getReservationId( ),
        Integer.valueOf( instance.getLaunchIndex( ) ),
        instance.getInstanceType( ).getName( ),
        instance.getInstanceType( ).getCores( ),
        instance.getInstanceType( ).getDisk( ),
        instance.getInstanceType( ).getMemory( ),
        instance.getPlatform( ),
        instance.getKeyName( ),
        instance.getLaunchTime( ).getTime( ),
        instance.getStateName( ),
        instance.getLaunchTime( ).getTime( ), //TODO: state time?
        null,
        instance.getNetParams( ).getAttachmentId( ),
        instance.getNetParams( ).getPrivateMacAddress( ),
        instance.getNetParams( ).getPrivateIp( ),
        null,
        instance.getOwnerId( ),
        instance.getAccountId( ),
        null
    );

    for ( final VolumeType volume : instance.getVolumes( ) ) {
      vmInfo.getVolumeAttachments( ).put( volume.getVolumeId( ), new VmVolumeAttachment(
          0, //TODO attach time?
          volume.getVolumeId( ),
          volume.getLocalDev( ),
          volume.getRemoteDev( ),
          volume.getState( )
      ) );
    }
    return vmInfo;
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

  public ArrayList<MetricsResourceType> getMetrics( ) {
    return metrics;
  }

  public void setMetrics( final ArrayList<MetricsResourceType> metrics ) {
    this.metrics = metrics;
  }

  public String state( final String state, final long timestamp ) {
    if ( state != null && ( getState( ) == null || !getState( ).equals( state ) ) ) {
      setState( state );
      setStateTimestamp( timestamp );
    }
    return getState( );
  }

  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "id", getId( ) )
        .add( "uuid", getUuid( ) )
        .add( "reservation-id", getReservationId( ) )
        .add( "owner-id", getOwnerId( ) )
        .add( "launch-index", getLaunchIndex( ) )
        .add( "launch-time", Timestamps.formatIso8601Timestamp( new Date( getLaunchtime( ) ) ) )
        .add( "instance-type-name", getInstanceTypeName( ) )
        .add( "state", getState( ) )
        .add( "vpc-id", getVpcId( ) )
        .omitNullValues( )
        .toString( );
  }
}
