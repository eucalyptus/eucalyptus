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
package com.eucalyptus.cluster.service.scheduler;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import com.eucalyptus.cluster.common.msgs.VmTypeInfo;
import com.eucalyptus.cluster.service.node.ClusterNode;
import com.eucalyptus.util.LockResource;
import javaslang.collection.Stream;
import javaslang.control.Option;

/**
 *
 */
public interface Scheduler {

  Lock schedulingLock = new ReentrantLock(  );

  String name( );

  Option<ClusterNode> schedule( Stream<ClusterNode> nodes, VmTypeInfo vmTypeInfo );

  static <R> R withLock( Supplier<R> schedulingAction ) {
    try ( final LockResource resource = LockResource.lock( schedulingLock ) ) {
      return schedulingAction.get( );
    }
  }

  static void adjust( final ClusterNode node ) {
    ScheduleResource.adjustAll( node );
  }

  static Predicate<ClusterNode> resourcesFor( final VmTypeInfo vmTypeInfo ) {
    return clusterNode ->
        clusterNode.getCoresAvailable( ) >= vmTypeInfo.getCores( ) &&
        clusterNode.getDiskAvailable( ) >= vmTypeInfo.getDisk( ) &&
        clusterNode.getMemoryAvailable( ) >= vmTypeInfo.getMemory( );
  }

  static Predicate<ClusterNode> reserve( final VmTypeInfo vmTypeInfo ) {
    return clusterNode -> withLock( ( ) -> {
      final Option<ScheduleResource> resourceOption = ScheduleResource.active( );
      boolean reserved = false;
      if ( resourceOption.isDefined( ) && resourcesFor( vmTypeInfo ).test( clusterNode ) ) {
        resourceOption.get( ).resources( vmTypeInfo.getCores( ), vmTypeInfo.getDisk( ), vmTypeInfo.getMemory( ) );
        resourceOption.get( ).apply( clusterNode );
        reserved = true;
      }
      return reserved;
    } );
  }
}
