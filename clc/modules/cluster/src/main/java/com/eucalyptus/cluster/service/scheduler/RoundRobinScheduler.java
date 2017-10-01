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

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.eucalyptus.cluster.service.node.ClusterNode;
import com.eucalyptus.cluster.service.vm.ClusterVmType;
import com.google.common.collect.Lists;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

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
      final ClusterVmType vmTypeResources
  ) {
    return Scheduler.withLock( ( ) -> {
      final Iterator<ClusterNode> nodesIterator = nodes.iterator( );
      final List<ClusterNode> head = Lists.newArrayList( );
      while ( nodesIterator.hasNext( ) ) {
        final ClusterNode node = nodesIterator.next( );
        head.add( node );
        if ( node.getNode( ).equals( lastNode.get( ) ) ) {
          break;
        }
      }
      final Option<ClusterNode> scheduled =
          Stream.ofAll( () -> nodesIterator ).appendAll( head ).find( Scheduler.reserve( vmTypeResources ) );
      scheduled.forEach( clusterNode -> lastNode.set( clusterNode.getNode( ) ) );
      return scheduled;
    } );
  }
}
