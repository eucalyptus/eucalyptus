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
package com.eucalyptus.cluster.service.scheduler;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.eucalyptus.cluster.common.msgs.VmTypeInfo;
import com.eucalyptus.cluster.service.node.ClusterNode;
import javaslang.collection.Stream;
import javaslang.control.Option;

/**
 *
 */
public class RoundRobinScheduler implements Scheduler {

  private AtomicReference<String> lastNode = new AtomicReference<>( "" );

  @Override
  public String name( ) {
    return "ROUNDROBIN";
  }

  @Override
  public Option<ClusterNode> schedule(
      final Stream<ClusterNode> nodes,
      final VmTypeInfo vmTypeInfo
  ) {
    return Scheduler.withLock( ( ) -> {
      final List<ClusterNode> head = nodes.takeWhile( node -> !node.getNode( ).equals( lastNode.get( ) ) ).toJavaList( );
      if ( !nodes.isEmpty( ) ) {
        head.add( nodes.get( ) );
      }
      Option<ClusterNode> scheduled = nodes.appendAll( head ).find( Scheduler.reserve( vmTypeInfo ) );
      scheduled.forEach( clusterNode -> lastNode.set( clusterNode.getNode( ) ) );
      return scheduled;
    } );
  }
}
