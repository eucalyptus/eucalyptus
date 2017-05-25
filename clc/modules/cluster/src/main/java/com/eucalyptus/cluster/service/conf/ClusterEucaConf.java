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
package com.eucalyptus.cluster.service.conf;

import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;
import com.google.common.base.MoreObjects;

/**
 * Cluster related eucalyptus.conf properties.
 */
public class ClusterEucaConf {
  private final long creationTime;
  private final String scheduler;
  private final Set<String> nodes;
  private final int nodePort;
  private final int maxInstances;

  public ClusterEucaConf(
      final long creationTime,
      final String scheduler,
      final Set<String> nodes,
      final int nodePort,
      final int maxInstances ) {
    this.creationTime = creationTime;
    this.scheduler = MoreObjects.firstNonNull( scheduler, "ROUNDROBIN" );
    this.nodes = MoreObjects.firstNonNull( nodes, Collections.emptySet( ) );
    this.nodePort = nodePort;
    this.maxInstances = maxInstances;
  }

  public long getCreationTime( ) {
    return creationTime;
  }

  public int getMaxInstances( ) {
    return maxInstances;
  }

  public int getNodePort( ) {
    return nodePort;
  }

  @Nonnull
  public Set<String> getNodes( ) {
    return nodes;
  }

  @Nonnull
  public String getScheduler( ) {
    return scheduler;
  }
}
