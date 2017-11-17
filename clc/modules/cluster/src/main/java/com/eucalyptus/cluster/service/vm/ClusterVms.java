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
import java.util.List;
import java.util.stream.Collectors;
import com.eucalyptus.cluster.common.msgs.AttachedVolume;
import com.eucalyptus.cluster.common.msgs.NetworkConfigType;
import com.eucalyptus.cluster.common.msgs.VirtualBootRecord;
import com.eucalyptus.cluster.common.msgs.VmInfo;
import com.eucalyptus.cluster.common.msgs.VmTypeInfo;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;

/**
 *
 */
public class ClusterVms {

  /**
   * Does not populate the service tag.
   */
  public static VmInfo vmToVmInfo( final ClusterVm vm ) {
    final ClusterVmBootRecord bootRecord = vm.getNodeBootRecord( ).or( vm.getBootRecord( ) );
    final VmTypeInfo vmTypeInfo = new VmTypeInfo( );
    vmTypeInfo.setName( vm.getVmType( ).getName( ) );
    vmTypeInfo.setCores( vm.getVmType( ).getCores( ) );
    vmTypeInfo.setDisk( vm.getVmType( ).getDisk( ) );
    vmTypeInfo.setMemory( vm.getVmType( ).getMemory( ) );
    vmTypeInfo.setVirtualBootRecord(
        Stream.ofAll( bootRecord.getDevices( ) )
          .map( ClusterVms::vmBootDeviceToVirtualBootRecord )
          .toJavaCollection( ArrayList::new )
    );

    final NetworkConfigType netConfig = new NetworkConfigType( );
    netConfig.setMacAddress( vm.getPrimaryInterface( ).getMac( ) );
    netConfig.setIpAddress( vm.getPrimaryInterface( ).getPrivateAddress( ) );
    netConfig.setIgnoredPublicIp( vm.getPrimaryInterface( ).getPublicAddress( ) );
    netConfig.setDevice( 0 );
    netConfig.setVlan( -1 );
    netConfig.setNetworkIndex( -1L );
    final List<NetworkConfigType> secondaryNetConfigList = Lists.newArrayList( );
    if ( vm.getVpcId( ) != null ) {
      netConfig.setInterfaceId( Strings.nullToEmpty( vm.getPrimaryInterface( ).getInterfaceId( ) ) );
      netConfig.setAttachmentId( Strings.nullToEmpty( vm.getPrimaryInterface( ).getAttachmentId( ) ) );

      // secondary interfaces here
      secondaryNetConfigList.addAll( vm.getSecondaryInterfaceAttachments( ).values( ).stream( ).map( fvi -> {
        NetworkConfigType secNetConfig = new NetworkConfigType( fvi.getInterfaceId( ), fvi.getDevice( ) );
        secNetConfig.setAttachmentId( fvi.getAttachmentId( ) );
        secNetConfig.setMacAddress( fvi.getMac( ) );
        secNetConfig.setIpAddress( fvi.getPrivateAddress( ) );
        secNetConfig.setIgnoredPublicIp( fvi.getPublicAddress( ) );
        secNetConfig.setVlan( -1 );
        secNetConfig.setNetworkIndex( -1L );
        return secNetConfig;
      } ).collect( Collectors.toList( ) ) );
    } else {
      netConfig.setInterfaceId( "" );
      netConfig.setAttachmentId( "" );
    }

    final List<AttachedVolume> volumes = Lists.newArrayList( );
    for ( final ClusterVmVolume volumeAttachment : vm.getVolumeAttachments( ).values( ) ) {
      final AttachedVolume volume = new AttachedVolume(  );
      volume.setVolumeId( volumeAttachment.getVolumeId( ) );
      volume.setInstanceId( vm.getId( ) );
      volume.setDevice( volumeAttachment.getDevice( ) );
      volume.setRemoteDevice( volumeAttachment.getRemoteDevice( ) );
      volume.setAttachTime( new Date( volumeAttachment.getAttachmentTimestamp( ) ) );
      volume.setStatus( volumeAttachment.getState( ) );
      volumes.add( volume );
    }

    final VmInfo vmInfo = new VmInfo( );
    vmInfo.setInstanceId( vm.getId( ) );
    vmInfo.setUuid( vm.getUuid( ) );
    vmInfo.setInstanceType( vmTypeInfo );
    vmInfo.setKeyValue( vm.getSshKeyValue( ) );
    vmInfo.setLaunchTime( new Date( vm.getLaunchtime( ) ) );
    vmInfo.setLaunchIndex( String.valueOf( vm.getLaunchIndex( ) ) );
    vmInfo.setStateName( vm.getState( ) );
    vmInfo.setNetParams( netConfig );
    vmInfo.getSecondaryNetConfigList( ).addAll( secondaryNetConfigList );
    vmInfo.setOwnerId( vm.getOwnerId( ) );
    vmInfo.setAccountId( vm.getAccountId( ) );
    vmInfo.setReservationId( vm.getReservationId( ) );
    vmInfo.setKeyValue( vm.getSshKeyValue( ) );
    vmInfo.setNetworkBytes( 0L );
    vmInfo.setBlockBytes( 0L );
    vmInfo.setPlatform( vm.getPlatform( ) );
    vmInfo.setGuestStateName( vm.getGuestState() );
    vmInfo.setBundleTaskStateName( vm.getBundleState( ).getState( ) );
    vmInfo.setBundleTaskProgress( vm.getBundleState( ).getProgress( ) );
    vmInfo.setMigrationStateName( vm.getMigrationState( ).getState( ) );
    vmInfo.setMigrationSource( vm.getMigrationState( ).getSourceHost( ) );
    vmInfo.setMigrationDestination( vm.getMigrationState( ).getDestinationHost( ) );
    vmInfo.getVolumes( ).addAll( volumes );

    return vmInfo;
  }

  @SuppressWarnings( "WeakerAccess" )
  public static VirtualBootRecord vmBootDeviceToVirtualBootRecord( final ClusterVmBootDevice bootDevice ) {
    final VirtualBootRecord bootRecord = new VirtualBootRecord( );
    bootRecord.setGuestDeviceName( bootDevice.getDevice( ) );
    bootRecord.setType( bootDevice.getType( ) );
    bootRecord.setSize( bootDevice.getSize( ) );
    bootRecord.setFormat( bootDevice.getFormat( ) );
    bootRecord.setId( bootDevice.getResource( ).map( Tuple2::_1 ).getOrElse( "none" ) );
    bootRecord.setResourceLocation( bootDevice.getResource( ).map( Tuple2::_2 ).getOrElse( "none" ) );
    return bootRecord;
  }
}
