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

import com.eucalyptus.cluster.common.msgs.BroadcastNetworkInfoResponseType;
import com.eucalyptus.cluster.common.msgs.BroadcastNetworkInfoType;
import com.eucalyptus.cluster.common.msgs.ClusterAttachVolumeResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterAttachVolumeType;
import com.eucalyptus.cluster.common.msgs.ClusterBundleInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterBundleInstanceType;
import com.eucalyptus.cluster.common.msgs.ClusterBundleRestartInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterBundleRestartInstanceType;
import com.eucalyptus.cluster.common.msgs.ClusterCancelBundleTaskResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterCancelBundleTaskType;
import com.eucalyptus.cluster.common.msgs.ClusterDescribeBundleTasksResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterDescribeBundleTasksType;
import com.eucalyptus.cluster.common.msgs.ClusterDetachVolumeResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterDetachVolumeType;
import com.eucalyptus.cluster.common.msgs.ClusterGetConsoleOutputResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterGetConsoleOutputType;
import com.eucalyptus.cluster.common.msgs.ClusterMigrateInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterMigrateInstancesType;
import com.eucalyptus.cluster.common.msgs.ClusterRebootInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterRebootInstancesType;
import com.eucalyptus.cluster.common.msgs.ClusterStartInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterStartInstanceType;
import com.eucalyptus.cluster.common.msgs.ClusterStopInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterStopInstanceType;
import com.eucalyptus.cluster.common.msgs.ClusterTerminateInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterTerminateInstancesType;
import com.eucalyptus.cluster.common.msgs.DescribeResourcesResponseType;
import com.eucalyptus.cluster.common.msgs.DescribeResourcesType;
import com.eucalyptus.cluster.common.msgs.DescribeSensorsResponseType;
import com.eucalyptus.cluster.common.msgs.DescribeSensorsType;
import com.eucalyptus.cluster.common.msgs.ModifyNodeResponseType;
import com.eucalyptus.cluster.common.msgs.ModifyNodeType;
import com.eucalyptus.cluster.common.msgs.VmDescribeResponseType;
import com.eucalyptus.cluster.common.msgs.VmDescribeType;
import com.eucalyptus.cluster.common.msgs.VmRunResponseType;
import com.eucalyptus.cluster.common.msgs.VmRunType;

/**
 *
 */
public interface ClusterService {
  BroadcastNetworkInfoResponseType broadcastNetworkInfo( BroadcastNetworkInfoType request );

  ClusterAttachVolumeResponseType attachVolume( ClusterAttachVolumeType request );

  ClusterDetachVolumeResponseType detachVolume( ClusterDetachVolumeType request );

  ClusterGetConsoleOutputResponseType getConsoleOutput( ClusterGetConsoleOutputType request );

  ClusterMigrateInstancesResponseType migrateInstancesResponseType( ClusterMigrateInstancesType request );

  ClusterBundleInstanceResponseType bundleInstance( ClusterBundleInstanceType request );

  ClusterBundleRestartInstanceResponseType bundleRestartInstance( ClusterBundleRestartInstanceType request );

  ClusterCancelBundleTaskResponseType cancelBundleTask( ClusterCancelBundleTaskType request );

  ClusterDescribeBundleTasksResponseType describeBundleTasks( ClusterDescribeBundleTasksType request );

  ClusterRebootInstancesResponseType rebootInstances( ClusterRebootInstancesType request );

  ClusterStartInstanceResponseType startInstance( ClusterStartInstanceType request );

  ClusterStopInstanceResponseType stopInstance( ClusterStopInstanceType request );

  ClusterTerminateInstancesResponseType terminateInstances( ClusterTerminateInstancesType request );

  DescribeResourcesResponseType describeResources( DescribeResourcesType request );

  DescribeSensorsResponseType describeSensors( DescribeSensorsType request );

  VmDescribeResponseType describeVms( VmDescribeType request );

  VmRunResponseType runVm( VmRunType request );

  ModifyNodeResponseType modifyNode( ModifyNodeType request );
}
