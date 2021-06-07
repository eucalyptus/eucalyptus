/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.cluster.service;

import java.util.List;
import com.eucalyptus.cluster.common.msgs.InstanceType;
import com.eucalyptus.cluster.common.msgs.NcAssignAddressResponseType;
import com.eucalyptus.cluster.common.msgs.NcAssignAddressType;
import com.eucalyptus.cluster.common.msgs.NcAttachNetworkInterfaceResponseType;
import com.eucalyptus.cluster.common.msgs.NcAttachNetworkInterfaceType;
import com.eucalyptus.cluster.common.msgs.NcAttachVolumeResponseType;
import com.eucalyptus.cluster.common.msgs.NcAttachVolumeType;
import com.eucalyptus.cluster.common.msgs.NcBroadcastNetworkInfoResponseType;
import com.eucalyptus.cluster.common.msgs.NcBroadcastNetworkInfoType;
import com.eucalyptus.cluster.common.msgs.NcBundleInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.NcBundleInstanceType;
import com.eucalyptus.cluster.common.msgs.NcBundleRestartInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.NcBundleRestartInstanceType;
import com.eucalyptus.cluster.common.msgs.NcCancelBundleTaskResponseType;
import com.eucalyptus.cluster.common.msgs.NcCancelBundleTaskType;
import com.eucalyptus.cluster.common.msgs.NcCreateImageResponseType;
import com.eucalyptus.cluster.common.msgs.NcCreateImageType;
import com.eucalyptus.cluster.common.msgs.NcDescribeInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.NcDescribeInstancesType;
import com.eucalyptus.cluster.common.msgs.NcDescribeResourceResponseType;
import com.eucalyptus.cluster.common.msgs.NcDescribeResourceType;
import com.eucalyptus.cluster.common.msgs.NcDescribeSensorsResponseType;
import com.eucalyptus.cluster.common.msgs.NcDescribeSensorsType;
import com.eucalyptus.cluster.common.msgs.NcDetachNetworkInterfaceResponseType;
import com.eucalyptus.cluster.common.msgs.NcDetachNetworkInterfaceType;
import com.eucalyptus.cluster.common.msgs.NcDetachVolumeResponseType;
import com.eucalyptus.cluster.common.msgs.NcDetachVolumeType;
import com.eucalyptus.cluster.common.msgs.NcGetConsoleOutputResponseType;
import com.eucalyptus.cluster.common.msgs.NcGetConsoleOutputType;
import com.eucalyptus.cluster.common.msgs.NcMigrateInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.NcMigrateInstancesType;
import com.eucalyptus.cluster.common.msgs.NcModifyNodeResponseType;
import com.eucalyptus.cluster.common.msgs.NcModifyNodeType;
import com.eucalyptus.cluster.common.msgs.NcPowerDownResponseType;
import com.eucalyptus.cluster.common.msgs.NcPowerDownType;
import com.eucalyptus.cluster.common.msgs.NcRebootInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.NcRebootInstanceType;
import com.eucalyptus.cluster.common.msgs.NcRunInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.NcRunInstanceType;
import com.eucalyptus.cluster.common.msgs.NcStartInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.NcStartInstanceType;
import com.eucalyptus.cluster.common.msgs.NcStartNetworkResponseType;
import com.eucalyptus.cluster.common.msgs.NcStartNetworkType;
import com.eucalyptus.cluster.common.msgs.NcStopInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.NcStopInstanceType;
import com.eucalyptus.cluster.common.msgs.NcTerminateInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.NcTerminateInstanceType;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.google.common.collect.Lists;

/**
 * Node service API
 */
@SuppressWarnings( { "UnusedReturnValue", "unused" } )
public interface NodeService {

  // sync

  NcAssignAddressResponseType assignAddress( NcAssignAddressType request );
  NcAttachNetworkInterfaceResponseType attachNetworkInterface( NcAttachNetworkInterfaceType request );
  NcAttachVolumeResponseType attachVolume( NcAttachVolumeType request );
  NcBroadcastNetworkInfoResponseType broadcastNetworkInfo( NcBroadcastNetworkInfoType request );
  NcBundleInstanceResponseType bundleInstance( NcBundleInstanceType request );
  NcBundleRestartInstanceResponseType bundleRestartInstance( NcBundleRestartInstanceType request );
  NcCancelBundleTaskResponseType cancelBundleTask( NcCancelBundleTaskType request );
  NcCreateImageResponseType createImage( NcCreateImageType request );
  NcDescribeInstancesResponseType describeInstances( NcDescribeInstancesType request );
  NcDescribeResourceResponseType describeResource( NcDescribeResourceType request );
  NcDescribeSensorsResponseType describeSensors( NcDescribeSensorsType request );
  NcDetachNetworkInterfaceResponseType detachNetworkInterface( NcDetachNetworkInterfaceType request );
  NcDetachVolumeResponseType detachVolume( NcDetachVolumeType request );
  NcGetConsoleOutputResponseType getConsoleOutput( NcGetConsoleOutputType request );
  NcMigrateInstancesResponseType migrateInstances( NcMigrateInstancesType request );
  NcModifyNodeResponseType modifyNode( NcModifyNodeType request );
  NcPowerDownResponseType powerDown( NcPowerDownType request );
  NcRebootInstanceResponseType rebootInstance( NcRebootInstanceType request );
  NcRunInstanceResponseType runInstance( NcRunInstanceType request );
  NcStartInstanceResponseType startInstance( NcStartInstanceType request );
  NcStartNetworkResponseType startNetwork( NcStartNetworkType request );
  NcStopInstanceResponseType stopInstance( NcStopInstanceType request );
  NcTerminateInstanceResponseType terminateInstance( NcTerminateInstanceType request );

  // selected async

  CheckedListenableFuture<NcAssignAddressResponseType> assignAddressAsync( NcAssignAddressType request );
  CheckedListenableFuture<NcBroadcastNetworkInfoResponseType> broadcastNetworkInfoAsync( NcBroadcastNetworkInfoType request );
  CheckedListenableFuture<NcDescribeInstancesResponseType> describeInstancesAsync( NcDescribeInstancesType request );
  CheckedListenableFuture<NcDescribeResourceResponseType> describeResourceAsync( NcDescribeResourceType request );
  CheckedListenableFuture<NcDescribeSensorsResponseType> describeSensorsAsync( NcDescribeSensorsType request );
  CheckedListenableFuture<NcMigrateInstancesResponseType> migrateInstancesAsync( NcMigrateInstancesType request );

  // convenience

  default NcAssignAddressResponseType assignAddress(
      final String instanceId,
      final String publicIp
  ) {
    final NcAssignAddressType assignAddress = new NcAssignAddressType( );
    assignAddress.setInstanceId( instanceId );
    assignAddress.setPublicIp( publicIp );
    return assignAddress( assignAddress );
  }

  default CheckedListenableFuture<NcAssignAddressResponseType> assignAddressAsync(
      final String instanceId,
      final String publicIp
  ) {
    final NcAssignAddressType assignAddress = new NcAssignAddressType( );
    assignAddress.setInstanceId( instanceId );
    assignAddress.setPublicIp( publicIp );
    return assignAddressAsync( assignAddress );
  }

  default NcGetConsoleOutputResponseType getConsoleOutput(
      final String instanceId
  ) {
    final NcGetConsoleOutputType getConsoleOutput = new NcGetConsoleOutputType( );
    getConsoleOutput.setInstanceId( instanceId );
    return getConsoleOutput( getConsoleOutput );
  }

  default NcRebootInstanceResponseType rebootInstance(
      final String instanceId
  ) {
    final NcRebootInstanceType rebootInstance = new NcRebootInstanceType( );
    rebootInstance.setInstanceId( instanceId );
    return rebootInstance( rebootInstance );
  }

  default NcStartInstanceResponseType startInstance(
      final String instanceId
  ) {
    final NcStartInstanceType startInstance = new NcStartInstanceType( );
    startInstance.setInstanceId( instanceId );
    return startInstance( startInstance );
  }

  default NcStopInstanceResponseType stopInstance(
      final String instanceId
  ) {
    final NcStopInstanceType stopInstance = new NcStopInstanceType( );
    stopInstance.setInstanceId( instanceId );
    return stopInstance( stopInstance );
  }

  @SuppressWarnings( "SameParameterValue" )
  default NcTerminateInstanceResponseType terminateInstance(
      final String instanceId,
      final boolean force
  ) {
    final NcTerminateInstanceType terminateInstance = new NcTerminateInstanceType( );
    terminateInstance.setInstanceId( instanceId );
    terminateInstance.setForce( force );
    return terminateInstance( terminateInstance );
  }

  default NcMigrateInstancesResponseType migrateInstancesPrepare(
      final String credentials,
      final List<String> resourceLocation,
      final List<InstanceType> instances
  ) {
    final NcMigrateInstancesType migrateInstances = new NcMigrateInstancesType( );
    migrateInstances.setAction( "prepare" );
    migrateInstances.setCredentials( credentials );
    migrateInstances.setResourceLocation( Lists.newArrayList( resourceLocation ) );
    migrateInstances.setInstances( Lists.newArrayList( instances ) );
    return migrateInstances( migrateInstances );
  }

  default CheckedListenableFuture<NcMigrateInstancesResponseType> migrateInstancesPrepareAsync(
      final String credentials,
      final List<String> resourceLocation,
      final List<InstanceType> instances
  ) {
    final NcMigrateInstancesType migrateInstances = new NcMigrateInstancesType( );
    migrateInstances.setAction( "prepare" );
    migrateInstances.setCredentials( credentials );
    migrateInstances.setResourceLocation( Lists.newArrayList( resourceLocation ) );
    migrateInstances.setInstances( Lists.newArrayList( instances ) );
    return migrateInstancesAsync( migrateInstances );
  }

  default CheckedListenableFuture<NcMigrateInstancesResponseType> migrateInstancesActionAsync(
      final String action,
      final InstanceType instance
  ) {
    final NcMigrateInstancesType migrateInstances = new NcMigrateInstancesType( );
    migrateInstances.setAction( action );
    migrateInstances.setInstances( Lists.newArrayList( instance ) );
    return migrateInstancesAsync( migrateInstances );
  }
}
