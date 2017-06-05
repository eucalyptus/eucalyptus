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
  private final int instanceTimeout;

  public ClusterEucaConf(
      final long creationTime,
      final String scheduler,
      final Set<String> nodes,
      final int nodePort,
      final int maxInstances,
      final int instanceTimeout ) {
    this.creationTime = creationTime;
    this.scheduler = MoreObjects.firstNonNull( scheduler, "ROUNDROBIN" );
    this.nodes = MoreObjects.firstNonNull( nodes, Collections.emptySet( ) );
    this.nodePort = nodePort;
    this.maxInstances = maxInstances;
    this.instanceTimeout = instanceTimeout;
  }

  public long getCreationTime( ) {
    return creationTime;
  }

  public int getMaxInstances( ) {
    return maxInstances;
  }

  /**
   * Instance timeout in seconds.
   */
  public int getInstanceTimeout( ) {
    return instanceTimeout;
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
