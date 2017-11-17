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
