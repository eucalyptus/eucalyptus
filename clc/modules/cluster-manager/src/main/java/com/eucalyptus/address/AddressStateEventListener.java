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
package com.eucalyptus.address;

import static com.eucalyptus.entities.AbstractPersistent_.lastUpdateTimestamp;
import static com.eucalyptus.entities.AbstractStatefulPersistent_.state;
import static com.eucalyptus.entities.Entities.criteriaQuery;
import static com.eucalyptus.entities.Entities.restriction;
import static com.eucalyptus.entities.Entities.transactionFor;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.internal.address.AddressState;
import com.eucalyptus.compute.common.internal.address.AllocatedAddressEntity;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.network.NetworkGroups;

/**
 *
 */
public class AddressStateEventListener implements EventListener<ClockTick> {

  private static final Logger logger = Logger.getLogger( AddressStateEventListener.class );

  private static final Semaphore instanceTimeoutSemaphore = new Semaphore( 1 );

  protected void timeoutPendingAddresses( final Addresses addresses ) {
    if ( !instanceTimeoutSemaphore.tryAcquire( ) ) {
      return;
    }
    try {
      final long expiredTimestamp = System.currentTimeMillis( ) -
          TimeUnit.MINUTES.toMillis( NetworkGroups.ADDRESS_PENDING_TIMEOUT );
      final List<AllocatedAddressEntity> expiredPendingAddresses;
      try ( final TransactionResource tx = transactionFor( AllocatedAddressEntity.class ) ) {
        expiredPendingAddresses = criteriaQuery( AllocatedAddressEntity.class )
            .where( restriction( AllocatedAddressEntity.class )
                .before( lastUpdateTimestamp, new Date( expiredTimestamp ) )
                .equal( state, AddressState.impending ) )
            .readonly( )
            .list( );
      }
      for ( final AllocatedAddressEntity entity : expiredPendingAddresses ) {
        try {
          final Address address = addresses.lookupActiveAddress( entity.getDisplayName( ) );
          if ( address.getState( ) == AddressState.impending ) {
            logger.info( "Releasing pending address due to timeout: " + address.getDisplayName( ) );
            addresses.release( address, entity.getAllocationId( ) );
          }
        } catch ( NoSuchElementException e ) {
          // continue
        }
      }
    } finally {
      instanceTimeoutSemaphore.release( );
    }
  }

  public static void register( ) {
    Listeners.register( ClockTick.class, new AddressStateEventListener() );
  }

  @Override
  public void fireEvent( final ClockTick event ) {
    if ( Bootstrap.isOperational( ) && Topology.isEnabledLocally( Eucalyptus.class ) ) {
      timeoutPendingAddresses( Addresses.getInstance( ) );
    }
  }
}
