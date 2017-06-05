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

import com.eucalyptus.cluster.common.msgs.VirtualMachineType;
import com.eucalyptus.cluster.common.msgs.VmTypeInfo;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.eucalyptus.util.Assert;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class ClusterVmType {

  private final String name;
  private final int cores;
  private final int disk;
  private final int memory;

  private ClusterVmType( final String name, final int cores, final int disk, final int memory ) {
    this.name = Assert.notNull( name, "name" );
    this.cores = Assert.arg( cores, cores >=0, "illegal cores (%d)", cores );
    this.disk = Assert.arg( disk, disk >=0, "illegal disk (%d)", disk );
    this.memory = Assert.arg( memory, memory >=0, "illegal memory (%d)", memory );
  }

  public String getName( ) {
    return name;
  }

  /**
   * Core count
   */
  public int getCores( ) {
    return cores;
  }

  /**
   * Disk in GB
   */
  public int getDisk( ) {
    return disk;
  }

  /**
   * Memory in MB
   */
  public int getMemory( ) {
    return memory;
  }

  public static ClusterVmType of( final String name, final int cores, final int disk, final int memory ) {
    return new ClusterVmType( name, cores, disk, memory );
  }

  public static ClusterVmType from( final VmTypeInfo vmTypeInfo ) {
    return ClusterVmType.of(
        vmTypeInfo.getName( ),
        vmTypeInfo.getCores( ),
        vmTypeInfo.getDisk( ),
        vmTypeInfo.getMemory( ) );
  }

  public static ClusterVmType from( final VmType vmType ) {
    return ClusterVmType.of(
         vmType.getName( ),
         vmType.getCpu( ),
         vmType.getDisk( ),
         vmType.getMemory( ) );
  }

  public static ClusterVmType from( final VirtualMachineType vmType ) {
    return ClusterVmType.of(
        vmType.getName( ),
        vmType.getCores( ),
        vmType.getDisk( ),
        vmType.getMemory( ) );
  }

  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "name", getName( ) )
        .add( "cores", getCores( ) )
        .add( "disk", getDisk( ) )
        .add( "memory", getMemory( ) )
        .toString( );
  }
}
