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
package com.eucalyptus.cluster.service.node;

import java.util.List;
import com.eucalyptus.cluster.service.vm.VmInfo;
import com.google.common.collect.Lists;
import javaslang.collection.Stream;

/**
 *
 */
public final class ClusterNode {
  private final String node;

  private String iqn;
  private int coresAvailable;
  private int coresTotal;
  private int diskAvailable;
  private int diskTotal;
  private int memoryAvailable;
  private int memoryTotal;
  private String nodeStatus;
  private Boolean migrationCapable;
  private String publicSubnets;
  private String hypervisor;

  private List<VmInfo> vms = Lists.newCopyOnWriteArrayList( );

  public ClusterNode( final String node ) {
    this( node, null, 0, 0, 0 );
  }

  public ClusterNode( final String node,
                      final String iqn,
                      final int cores,
                      final int disk,
                      final int memory
  ) {
    this.node = node;
    this.nodeStatus = "LOADED";
    this.iqn = iqn;
    this.coresAvailable = this.coresTotal = cores;
    this.diskAvailable = this.diskTotal = disk;
    this.memoryAvailable = this.memoryTotal = memory;
  }

  public String getIqn( ) {
    return iqn;
  }

  public void setIqn( final String iqn ) {
    this.iqn = iqn;
  }

  public int getCoresAvailable( ) {
    return coresAvailable;
  }

  public void setCoresAvailable( final int coresAvailable ) {
    this.coresAvailable = coresAvailable;
  }

  public int getCoresTotal( ) {
    return coresTotal;
  }

  public void setCoresTotal( final int coresTotal ) {
    this.coresTotal = coresTotal;
  }

  public int getDiskAvailable( ) {
    return diskAvailable;
  }

  public void setDiskAvailable( final int diskAvailable ) {
    this.diskAvailable = diskAvailable;
  }

  public int getDiskTotal( ) {
    return diskTotal;
  }

  public void setDiskTotal( final int diskTotal ) {
    this.diskTotal = diskTotal;
  }

  public int getMemoryAvailable( ) {
    return memoryAvailable;
  }

  public void setMemoryAvailable( final int memoryAvailable ) {
    this.memoryAvailable = memoryAvailable;
  }

  public int getMemoryTotal( ) {
    return memoryTotal;
  }

  public void setMemoryTotal( final int memoryTotal ) {
    this.memoryTotal = memoryTotal;
  }

  public String getNodeStatus( ) {
    return nodeStatus;
  }

  public void setNodeStatus( final String nodeStatus ) {
    this.nodeStatus = nodeStatus;
  }

  public Boolean getMigrationCapable( ) {
    return migrationCapable;
  }

  public void setMigrationCapable( final Boolean migrationCapable ) {
    this.migrationCapable = migrationCapable;
  }

  public String getPublicSubnets( ) {
    return publicSubnets;
  }

  public void setPublicSubnets( final String publicSubnets ) {
    this.publicSubnets = publicSubnets;
  }

  public String getHypervisor( ) {
    return hypervisor;
  }

  public void setHypervisor( final String hypervisor ) {
    this.hypervisor = hypervisor;
  }

  public String getNode( ) {
    return node;
  }

  public String getServiceTag( ) {
    return "http://" + node + ":8775/axis2/services/EucalyptusNC";
  }

  public Stream<VmInfo> getVms( ) {
    return Stream.ofAll( vms );
  }

  public VmInfo vm( final VmInfo vm ) {
    vms.add( vm );
    return vm;
  }
}
