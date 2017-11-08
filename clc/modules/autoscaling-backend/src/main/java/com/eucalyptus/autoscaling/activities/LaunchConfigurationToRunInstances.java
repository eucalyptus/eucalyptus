/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
