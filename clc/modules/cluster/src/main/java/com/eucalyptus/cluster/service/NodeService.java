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
package com.eucalyptus.cluster.service;

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

/**
 * Node service API
 */
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

  CheckedListenableFuture<NcBroadcastNetworkInfoResponseType> broadcastNetworkInfoAsync( NcBroadcastNetworkInfoType request );
  CheckedListenableFuture<NcDescribeInstancesResponseType> describeInstancesAsync( NcDescribeInstancesType request );
  CheckedListenableFuture<NcDescribeResourceResponseType> describeResourceAsync( NcDescribeResourceType request );
  CheckedListenableFuture<NcDescribeSensorsResponseType> describeSensorsAsync( NcDescribeSensorsType request );

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

  default NcTerminateInstanceResponseType terminateInstance(
      final String instanceId,
      final boolean force
  ) {
    final NcTerminateInstanceType terminateInstance = new NcTerminateInstanceType( );
    terminateInstance.setInstanceId( instanceId );
    terminateInstance.setForce( force );
    return terminateInstance( terminateInstance );
  }

}
