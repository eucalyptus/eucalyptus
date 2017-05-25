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

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import com.eucalyptus.cluster.service.node.ClusterNode;
import com.google.common.collect.Sets;
import javaslang.control.Option;

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
      clusterNode.setCoresAvailable( clusterNode.getCoresAvailable( ) - (cores * factor) );
      clusterNode.setDiskAvailable( clusterNode.getDiskAvailable( ) - (disk * factor) );
      clusterNode.setMemoryAvailable( clusterNode.getMemoryAvailable( ) - (memory * factor) );
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
