/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
