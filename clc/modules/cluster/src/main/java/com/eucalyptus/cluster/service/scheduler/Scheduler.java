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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.Supplier;
import com.eucalyptus.cluster.service.node.ClusterNode;
import com.eucalyptus.cluster.service.vm.ClusterVmType;
import com.eucalyptus.util.LockResource;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

/**
 *
 */
public interface Scheduler {

  Lock schedulingLock = new ReentrantLock(  );

  String name( );

  Option<ClusterNode> schedule( Stream<ClusterNode> nodes, ClusterVmType vmTypeResources );

  static <R> R withLock( Supplier<R> schedulingAction ) {
    try ( final LockResource resource = LockResource.lock( schedulingLock ) ) {
      return schedulingAction.get( );
    }
  }

  static void adjust( final ClusterNode node ) {
    ScheduleResource.adjustAll( node );
  }

  static Predicate<ClusterNode> resourcesFor( final ClusterVmType vmTypeResources ) {
    return clusterNode ->
        clusterNode.getCoresAvailable( ) >= vmTypeResources.getCores( ) &&
        clusterNode.getDiskAvailable( ) >= vmTypeResources.getDisk( ) &&
        clusterNode.getMemoryAvailable( ) >= vmTypeResources.getMemory( );
  }

  static Predicate<ClusterNode> reserve( final ClusterVmType vmTypeResources ) {
    return clusterNode -> withLock( ( ) -> {
      final Option<ScheduleResource> resourceOption = ScheduleResource.active( );
      boolean reserved = false;
      if ( resourceOption.isDefined( ) && resourcesFor( vmTypeResources ).test( clusterNode ) ) {
        resourceOption.get( ).resources( vmTypeResources.getCores( ), vmTypeResources.getDisk( ), vmTypeResources.getMemory( ) );
        resourceOption.get( ).apply( clusterNode );
        reserved = true;
      }
      return reserved;
    } );
  }
}
