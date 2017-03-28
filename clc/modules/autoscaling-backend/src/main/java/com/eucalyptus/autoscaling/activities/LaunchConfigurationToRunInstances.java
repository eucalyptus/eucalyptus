/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.autoscaling.activities;

import static com.eucalyptus.autoscaling.common.internal.configurations.LaunchConfigurations.containsSecurityGroupIdentifiers;
import java.util.ArrayList;
import com.eucalyptus.autoscaling.common.internal.configurations.BlockDeviceMapping;
import com.eucalyptus.autoscaling.common.internal.configurations.LaunchConfigurationCoreView;
import com.eucalyptus.compute.common.BlockDeviceMappingItemType;
import com.eucalyptus.compute.common.EbsDeviceMapping;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetItemRequestType;
import com.eucalyptus.compute.common.backend.RunInstancesType;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 *
 */
@TypeMapper
public enum LaunchConfigurationToRunInstances implements Function<LaunchConfigurationCoreView, RunInstancesType> {
  INSTANCE;

  @Override
  public RunInstancesType apply( final LaunchConfigurationCoreView launchConfiguration ) {
    final RunInstancesType runInstances = new RunInstancesType( );
    runInstances.setKernelId( launchConfiguration.getKernelId( ) );
    runInstances.setRamdiskId( launchConfiguration.getRamdiskId( ) );
    runInstances.setImageId( launchConfiguration.getImageId( ) );
    runInstances.setInstanceType( launchConfiguration.getInstanceType( ) );
    runInstances.setMinCount( 1 );
    runInstances.setMaxCount( 1 );
    for ( final BlockDeviceMapping mapping : launchConfiguration.getBlockDeviceMappings( ) ) {
      final BlockDeviceMappingItemType type = new BlockDeviceMappingItemType( );
      type.setDeviceName( mapping.getDeviceName( ) );
      type.setVirtualName( mapping.getVirtualName( ) );
      if ( mapping.getSnapshotId( ) != null || mapping.getVolumeSize( ) != null ) {
        final EbsDeviceMapping ebsType = new EbsDeviceMapping( );
        ebsType.setSnapshotId( mapping.getSnapshotId( ) );
        ebsType.setVolumeSize( mapping.getVolumeSize( ) );
        ebsType.setDeleteOnTermination( true );
        type.setEbs( ebsType );
      }
      runInstances.getBlockDeviceMapping( ).add( type );
    }
    runInstances.setKeyName( launchConfiguration.getKeyName( ) );
    final ArrayList<String> securityGroups =
        Lists.newArrayList( launchConfiguration.getSecurityGroups( ) );
    if ( launchConfiguration.getAssociatePublicIpAddress( ) != null ) {
      final InstanceNetworkInterfaceSetItemRequestType networkInterface =
          runInstances.primaryNetworkInterface( true );
      networkInterface.setAssociatePublicIpAddress( launchConfiguration.getAssociatePublicIpAddress( ) );
      networkInterface.securityGroups( securityGroups );
    } else {
      if ( containsSecurityGroupIdentifiers( securityGroups ) ) {
        runInstances.setGroupIdSet( securityGroups );
      } else {
        runInstances.setGroupSet( securityGroups );
      }
    }
    runInstances.setMonitoring( launchConfiguration.getInstanceMonitoring( ) );
    if ( launchConfiguration.getIamInstanceProfile( ) != null ) {
      runInstances.setInstanceProfileNameOrArn( launchConfiguration.getIamInstanceProfile( ) );
    }
    runInstances.setUserData( launchConfiguration.getUserData( ) );
    return runInstances;
  }
}
