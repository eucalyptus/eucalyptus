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
