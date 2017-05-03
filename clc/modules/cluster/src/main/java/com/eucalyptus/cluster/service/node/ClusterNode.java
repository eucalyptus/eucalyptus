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

/**
 *
 */
public final class ClusterNode {
  private final String node;
  private final String iqn = "iqn.1994-05.com.redhat:c7ec6fad289";
  private final int cores;
  private final int disk;
  private final int memory;
  private final List<VmInfo> vms = Lists.newCopyOnWriteArrayList( );

  public ClusterNode( final String node,
                      final int cores,
                      final int disk,
                      final int memory
  ) {
    this.node = node;
    this.cores = cores;
    this.disk = disk;
    this.memory = memory;
  }

  public int getCores() {
    return cores;
  }

  public int getDisk() {
    return disk;
  }

  public String getIqn() {
    return iqn;
  }

  public int getMemory() {
    return memory;
  }

  public String getNode() {
    return node;
  }

  public List<VmInfo> getVms() {
    return vms;
  }

  public String getServiceTag() {
    return "http://" + node + ":8775/axis2/services/EucalyptusNC";
  }
}
