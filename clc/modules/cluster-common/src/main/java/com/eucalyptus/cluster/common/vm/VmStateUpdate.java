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
package com.eucalyptus.cluster.common.vm;

import java.util.List;
import java.util.Set;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.msgs.VmInfo;

/**
 *
 */
public class VmStateUpdate {

  private final Cluster cluster;
  private final Set<String> requestedVms;
  private final List<VmInfo> vmInfos;

  public VmStateUpdate( final Cluster cluster, final Set<String> requestedVms, final List<VmInfo> vmInfos ) {
    this.cluster = cluster;
    this.requestedVms = requestedVms;
    this.vmInfos = vmInfos;
  }

  public Cluster getCluster( ) {
    return cluster;
  }

  public Set<String> getRequestedVms( ) {
    return requestedVms;
  }

  public List<VmInfo> getVmInfos( ) {
    return vmInfos;
  }
}
