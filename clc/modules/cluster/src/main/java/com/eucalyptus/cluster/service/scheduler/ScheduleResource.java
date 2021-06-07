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

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import com.eucalyptus.cluster.service.node.ClusterNode;
import com.google.common.collect.Sets;
import io.vavr.control.Option;

/**
 *
 */
public class ScheduleResource implements AutoCloseable {

  private static final ThreadLocal<ScheduleResource> threadScheduleResource = new ThreadLocal<>( );
  private static final Set<ScheduleResource> activeScheduleResources = Sets.newConcurrentHashSet( );

  private final AtomicBoolean complete = new AtomicBoolean( false );

  private volatile String node;
  private volatile int cores;
  private volatile int disk;
  private volatile int memory;
  private volatile Runnable rollback;

  static Option<ScheduleResource> active( ) {
    return Option.of( threadScheduleResource.get( ) );
  }

  static void adjustAll( final ClusterNode node ) {
    activeScheduleResources.forEach( resource -> resource.adjust( node ) );
  }

  ScheduleResource( ) {
    threadScheduleResource.set( this );
    activeScheduleResources.add( this );
  }

  public void commit( ) {
    complete( );
  }

  public void rollback( ) {
    final Runnable rollback = this.rollback;
    Scheduler.withLock( () -> {
      if ( complete( ) && rollback != null ) {
        rollback.run( );
      }
      return true;
    } );
  }

  @Override
  public void close( ) {
    rollback( );
  }

  void resources( final int cores, final int disk, final int memory ) {
    this.cores = cores;
    this.disk = disk;
    this.memory = memory;
  }

  void apply( final ClusterNode clusterNode ) {
    this.node = clusterNode.getNode( );
    adjust( clusterNode );
    this.rollback = ( ) -> adjustWithFactor( clusterNode, -1 );
  }

  void adjust( final ClusterNode clusterNode ) {
    adjustWithFactor( clusterNode, 1 );
  }

  private void adjustWithFactor( final ClusterNode clusterNode, final int factor ) {
    if ( clusterNode.getNode( ).equals( this.node ) ) {
      clusterNode.setAvailability(
          clusterNode.getCoresAvailable( ) - (cores * factor),
          clusterNode.getDiskAvailable( ) - (disk * factor),
          clusterNode.getMemoryAvailable( ) - (memory * factor) );
    }
  }

  private boolean complete( ) {
    final boolean completing = complete.compareAndSet( false, true );
    if ( completing ) {
      threadScheduleResource.set( null );
      activeScheduleResources.remove( this );
    }
    return completing;
  }
}
