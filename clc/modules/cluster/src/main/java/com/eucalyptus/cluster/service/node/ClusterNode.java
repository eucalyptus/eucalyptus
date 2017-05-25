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
