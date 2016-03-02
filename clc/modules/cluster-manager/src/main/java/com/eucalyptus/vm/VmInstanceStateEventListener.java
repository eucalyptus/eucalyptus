/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.vm;

import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.google.common.collect.Sets;

/**
 *
 */
public class VmInstanceStateEventListener implements EventListener<ClockTick> {

  private static final Logger logger = Logger.getLogger( VmInstanceStateEventListener.class );

  private static final Semaphore instanceTimeoutSemaphore = new Semaphore( 1 );

  public void timeoutPendingInstances( ) {
    if ( !instanceTimeoutSemaphore.tryAcquire( ) ) {
      return;
    }
    try {
      final Set<String> instanceIds = Sets.newHashSet( VmInstances.listWithProjection(
          VmInstances.instanceIdProjection( ),
          VmInstance.criterion( VmInstance.VmState.PENDING ),
          VmInstance.lastUpdatedCriterion(
              System.currentTimeMillis( ) - TimeUnit.MINUTES.toMillis( VmInstances.PENDING_TIME ) ),
          VmInstance.nullNodeCriterion( ) ) );

      for ( final String instanceId : instanceIds ) {
        logger.info( "Timing out pending instance : " + instanceId );
        try {
          VmInstances.terminated( instanceId );
        } catch ( final TransactionException e ) {
          logger.error( "Error terminating instance : " + instanceId, e );
        }
      }
    } finally {
      instanceTimeoutSemaphore.release( );
    }
  }

  public static void register( ) {
    Listeners.register( ClockTick.class, new VmInstanceStateEventListener() );
  }

  @Override
  public void fireEvent( final ClockTick event ) {
    if ( Bootstrap.isOperational( ) && Topology.isEnabledLocally( Eucalyptus.class ) ) {
      timeoutPendingInstances( );
    }
  }
}
