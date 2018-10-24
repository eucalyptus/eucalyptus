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
package com.eucalyptus.cluster.service.fake;

import java.time.Clock;
import java.util.concurrent.ConcurrentMap;
import com.eucalyptus.cluster.service.NodeService;
import com.eucalyptus.cluster.service.node.ClusterNode;
import com.eucalyptus.cluster.service.node.ClusterNodeServiceFactory;
import com.google.common.collect.Maps;

/**
 *
 */
public class FakeClusterNodeServiceFactory implements ClusterNodeServiceFactory {

  private final ConcurrentMap<String,NodeService> nodeServiceMap = Maps.newConcurrentMap( );
  private final boolean allowReload;
  private final Clock clock;

  public FakeClusterNodeServiceFactory( ) {
    this( Clock.systemDefaultZone( ), true );
  }

  public FakeClusterNodeServiceFactory( final Clock clock, final boolean allowReload ) {
    this.clock = clock;
    this.allowReload = allowReload;
  }


  @Override
  public NodeService nodeService( final ClusterNode node, final int port ) {
    return nodeServiceMap.computeIfAbsent( node.getNode( ), __ -> new FakeNodeService( node, clock, allowReload ) );
  }
}
