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
package com.eucalyptus.cluster.service.vm;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import com.eucalyptus.cluster.common.msgs.InstanceType;
import com.eucalyptus.cluster.common.msgs.MetricsResourceType;
import com.eucalyptus.cluster.common.msgs.NetConfigType;
import com.eucalyptus.cluster.common.msgs.VmRunType;
import com.eucalyptus.cluster.common.msgs.VolumeType;
import com.eucalyptus.crypto.util.Timestamps;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;

/**
 * 
 */
public final class ClusterVm {
  private final String id;
  private final String uuid;
  private final String reservationId;
  private final int launchIndex;
  private final ClusterVmType vmType;
  private final String platform;
  private final String sshKeyValue;
  private final long launchtime;
  private final ClusterVmBootRecord bootRecord;
  private final String ownerId;
  private final String accountId;

  private volatile long reportedTimestamp;
  private volatile String vpcId; // not present on launch
  private volatile ClusterVmState state;
  private volatile ClusterVmBundleState bundleState;
  private volatile ClusterVmMigrationState migrationState;
  private volatile ClusterVmMigrationState destinationMigrationState;
  private volatile ClusterVmBootRecord nodeBootRecord;
  private volatile ClusterVmInterface primaryInterface;
  private final ConcurrentMap<Integer, ClusterVmInterface> secondaryInterfaceAttachments = Maps.newConcurrentMap( );
  private final ConcurrentMap<String, ClusterVmVolume> volumeAttachments = Maps.newConcurrentMap( );

  private volatile ArrayList<MetricsResourceType> metrics = new ArrayList<>( );

  public ClusterVm(
      final String id,
      final String uuid,
      final String reservationId,
      final int launchIndex,
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
      final ClusterVmBootRecord bootRecord,
      final ClusterVmType vmType
  ) {
    this.id = id;
    this.uuid = uuid;
    this.reservationId = reservationId;
    this.launchIndex = launchIndex;
    this.vmType = vmType;
    this.platform = platform;
    this.sshKeyValue = sshKeyValue;
    this.launchtime = launchtime;
    this.reportedTimestamp = stateTimestamp;
    this.state = ClusterVmState.of( stateTimestamp, state, null );
    this.primaryInterface = ClusterVmInterface.of(
        interfaceId,
        attachmentId,
        0,
        mac,
        privateAddress,
        publicAddress
    );
    this.ownerId = ownerId;
    this.accountId = accountId;
    this.bootRecord = bootRecord;
    this.nodeBootRecord = ClusterVmBootRecord.none( );
    this.bundleState = ClusterVmBundleState.none( );
    this.migrationState = ClusterVmMigrationState.none( );
    this.destinationMigrationState = ClusterVmMigrationState.none( );
  }

  public static ClusterVm create( final VmRunType request, final long currentTime ) {
    final ClusterVm vmInfo = new ClusterVm(
        request.getInstanceId( ),
        request.getUuid( ),
        request.getReservationId( ),
        request.getLaunchIndex( ),
        request.getPlatform( ),
        request.getKeyInfo( ).getValue( ),
        currentTime,
        "Pending",
        currentTime,
        null,
        request.getPrimaryEniAttachmentId( ),
        request.getMacAddress( ),
        request.getPrivateAddress( ),
        null,
        request.getOwnerId( ),
        request.getAccountId( ),
        ClusterVmBootRecord.from( request.getVmTypeInfo( ).getVirtualBootRecord( ) ),
        ClusterVmType.from( request.getVmTypeInfo( ) )
    );
    request.getVmTypeInfo( ).getVirtualBootRecord( ).forEach( vbr -> {
      if ( Strings.nullToEmpty( vbr.getId( ) ).startsWith( "vol-" ) ) {
        vmInfo.getVolumeAttachments( ).put( vbr.getId( ), ClusterVmVolume.of(
            currentTime,
            vbr.getId( ),
            vbr.getGuestDeviceName( ),
            vbr.getResourceLocation( ),
            "attached"
        ) );
      }
    } );
    return vmInfo;
  }

  public static ClusterVm create( final InstanceType instance, final long currentTime ) {
    final ClusterVm vm = new ClusterVm(
        instance.getInstanceId( ),
        instance.getUuid( ),
        instance.getReservationId( ),
        Integer.valueOf( instance.getLaunchIndex( ) ),
        instance.getPlatform( ),
        instance.getKeyName( ),
        instance.getLaunchTime( ).getTime( ),
        instance.getStateName( ),
        currentTime,
        null,
        instance.getNetParams( ).getAttachmentId( ),
        instance.getNetParams( ).getPrivateMacAddress( ),
        instance.getNetParams( ).getPrivateIp( ),
        null,
        instance.getOwnerId( ),
        instance.getAccountId( ),
        ClusterVmBootRecord.fromNodeRecord( instance.getInstanceType( ).getVirtualBootRecord( ) ),
        ClusterVmType.from( instance.getInstanceType( ) )
    );

    vm.setNodeBootRecord( vm.getBootRecord( ) ); // main record is node record
    vm.state( instance.getStateName( ), instance.getGuestStateName( ), currentTime );
    vm.bundleState( instance.getBundleTaskStateName( ), instance.getBundleTaskProgress( ) );
    vm.migrateState( currentTime,
        instance.getMigrationStateName( ),
        instance.getMigrationSource( ),
        instance.getMigrationDestination( ) );

    for ( final NetConfigType netConfig : instance.getSecondaryNetConfig( ) ) {
      vm.getSecondaryInterfaceAttachments( ).put(
          netConfig.getDevice( ),
          ClusterVmInterface.fromNodeInterface( netConfig ) );
    }

    for ( final VolumeType volume : instance.getVolumes( ) ) {
      vm.getVolumeAttachments( ).put(
          volume.getVolumeId( ),
          ClusterVmVolume.fromNodeVolume( currentTime, volume ) );
    }

    return vm;
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

  public ClusterVmType getVmType() {
    return vmType;
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

  @SuppressWarnings( "WeakerAccess" )
  public ClusterVmBootRecord getBootRecord() {
    return bootRecord;
  }

  public ClusterVmInterface getPrimaryInterface() {
    return primaryInterface;
  }

  @SuppressWarnings( "WeakerAccess" )
  public void setPrimaryInterface( final ClusterVmInterface primaryInterface ) {
    this.primaryInterface = primaryInterface;
  }

  public ConcurrentMap<Integer, ClusterVmInterface> getSecondaryInterfaceAttachments() {
    return secondaryInterfaceAttachments;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public String getAccountId() {
    return accountId;
  }

  public long getReportedTimestamp( ) {
    return reportedTimestamp;
  }

  public void setReportedTimestamp( final long reportedTimestamp ) {
    this.reportedTimestamp = reportedTimestamp;
  }

  public String getVpcId() {
    return vpcId;
  }

  public void setVpcId( final String vpcId ) {
    this.vpcId = vpcId;
  }

  public String getState() {
    return state.getState( );
  }

  @SuppressWarnings( "unused" )
  public long getStateTimestamp( ) {
    return state.getTimestamp( );
  }

  @SuppressWarnings( "WeakerAccess" )
  @Nullable
  public String getGuestState( ) {
    return state.getGuestState( );
  }

  @SuppressWarnings( "WeakerAccess" )
  public ClusterVmBundleState getBundleState( ) {
    return bundleState;
  }

  @SuppressWarnings( "WeakerAccess" )
  public void setBundleState( final ClusterVmBundleState bundleState ) {
    this.bundleState = bundleState;
  }

  public ClusterVmMigrationState getMigrationState( ) {
    return migrationState;
  }

  @SuppressWarnings( "WeakerAccess" )
  public void setMigrationState( final ClusterVmMigrationState migrationState ) {
    this.migrationState = migrationState;
  }

  public ClusterVmMigrationState getDestinationMigrationState( ) {
    return destinationMigrationState;
  }

  @SuppressWarnings( "WeakerAccess" )
  public void setDestinationMigrationState( final ClusterVmMigrationState destinationMigrationState ) {
    this.destinationMigrationState = destinationMigrationState;
  }

  public ClusterVmBootRecord getNodeBootRecord() {
    return nodeBootRecord;
  }

  @SuppressWarnings( "WeakerAccess" )
  public void setNodeBootRecord( final ClusterVmBootRecord nodeBootRecord ) {
    this.nodeBootRecord = nodeBootRecord;
  }

  public ConcurrentMap<String, ClusterVmVolume> getVolumeAttachments() {
    return volumeAttachments;
  }

  public ArrayList<MetricsResourceType> getMetrics( ) {
    return metrics;
  }

  public void setMetrics( final ArrayList<MetricsResourceType> metrics ) {
    this.metrics = metrics;
  }

  public Option<Tuple2<ClusterVmState,ClusterVmState>> state( final String state, final String guestState, final long timestamp ) {
    Option<Tuple2<ClusterVmState,ClusterVmState>> stateChange = Option.none( );
    final ClusterVmState oldState = this.state;
    final ClusterVmState newState = ClusterVmState.of( timestamp, state, guestState );
    if ( !oldState.equals( newState ) ) {
      this.state = newState;
      stateChange = Option.of( Tuple.of( oldState, newState ) );
    }
    return stateChange;
  }

  public Option<Tuple2<ClusterVmBundleState,ClusterVmBundleState>> bundleState(
      final String state,
      final Double progress ) {
    final ClusterVmBundleState oldState = getBundleState( );
    setBundleState( ClusterVmBundleState.of(
        MoreObjects.firstNonNull( Strings.emptyToNull( state ), ClusterVmBundleState.none( ).getState( ) ),
        progress ) );
    final ClusterVmBundleState newState = getBundleState( );
    return oldState.equals( newState ) ?
        Option.none( ) :
        Option.of( Tuple.of( oldState, newState ) );
  }

  public Option<Tuple2<ClusterVmMigrationState,ClusterVmMigrationState>> migrateState(
      final long now,
      final String state,
      final String sourceHost,
      final String destinationHost
  ) {
    final ClusterVmMigrationState oldState = getMigrationState( );
    setMigrationState( ClusterVmMigrationState.of(
        ClusterVmMigrationState.timeForState( state, now ),
        state,
        Strings.emptyToNull( sourceHost ),
        Strings.emptyToNull( destinationHost ) ) );
    final ClusterVmMigrationState newState = getMigrationState( );
    return oldState.equals( newState ) ?
        Option.none( ) :
        Option.of( Tuple.of( oldState, newState ) );
  }

  public Option<Tuple2<ClusterVmMigrationState,ClusterVmMigrationState>> destinationMigrateState(
      final long now,
      final String state,
      final String sourceHost,
      final String destinationHost
  ) {
    final ClusterVmMigrationState oldState = getDestinationMigrationState( );
    setDestinationMigrationState( ClusterVmMigrationState.of(
        ClusterVmMigrationState.timeForState( state, now ),
        state,
        Strings.emptyToNull( sourceHost ),
        Strings.emptyToNull( destinationHost ) ) );
    final ClusterVmMigrationState newState = getDestinationMigrationState( );
    return oldState.equals( newState ) ?
        Option.none( ) :
        Option.of( Tuple.of( oldState, newState ) );
  }

  public Option<Tuple2<ClusterVmInterface,ClusterVmInterface>> primaryPublicAddress(
      final String address
  ) {
    Option<Tuple2<ClusterVmInterface,ClusterVmInterface>> result = Option.none( );
    final ClusterVmInterface oldInterface = getPrimaryInterface( );
    final ClusterVmInterface newInterface = oldInterface.withPublic( address );
    if ( !newInterface.equals( oldInterface ) ) {
      setPrimaryInterface( newInterface );
      result = Option.of( Tuple.of( oldInterface, newInterface ) );
    }
    return result;
  }

  public void updateBootRecord( final ClusterVmBootRecord bootRecord ) {
    if ( nodeBootRecord == null || !nodeBootRecord.equals( bootRecord ) ) {
      setNodeBootRecord( bootRecord==null ? ClusterVmBootRecord.none( ) : bootRecord );
    }
  }

  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "id", getId( ) )
        .add( "uuid", getUuid( ) )
        .add( "reservation-id", getReservationId( ) )
        .add( "owner-id", getOwnerId( ) )
        .add( "launch-index", getLaunchIndex( ) )
        .add( "launch-time", Timestamps.formatIso8601Timestamp( new Date( getLaunchtime( ) ) ) )
        .add( "instance-type", getVmType( ) )
        .add( "state", getState( ) )
        .add( "guest-state", getGuestState( ) )
        .add( "vpc-id", getVpcId( ) )
        .add( "bundle-info", getBundleState( ) )
        .add( "migrate-info", getMigrationState( ) )
        .omitNullValues( )
        .toString( );
  }
}
