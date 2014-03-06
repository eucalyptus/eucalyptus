/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.network

import com.eucalyptus.cloud.util.NotEnoughResourcesException
import com.eucalyptus.cloud.util.ResourceAllocationException
import com.eucalyptus.util.RestrictedTypes
import com.eucalyptus.vm.VmInstance
import com.google.common.collect.Iterables
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import java.util.concurrent.TimeUnit

import static com.eucalyptus.cloud.util.Reference.State.*

/**
 *
 */
@CompileStatic
abstract class PrivateAddressAllocatorSupport implements PrivateAddressAllocator {

  private final Logger logger
  private final PrivateAddressPersistence persistence

  protected PrivateAddressAllocatorSupport( final Logger logger,
                                            final PrivateAddressPersistence persistence ) {
    this.logger = logger
    this.persistence = persistence
  }

  @Override
  String allocate( Iterable<Integer> addresses ) throws NotEnoughResourcesException {
    allocate( addresses, { Integer address ->
      getDistinctPersistence( ).tryCreate( PrivateAddresses.fromInteger( address.intValue( ) ) )
          .transform( RestrictedTypes.toDisplayName( ) ).orNull( )
    } as Closure<String>) ?: typedThrow(String){ new NotEnoughResourcesException( 'Insufficient addresses' ) }
  }

  @Override
  void associate( String address, VmInstance instance ) throws ResourceAllocationException {
    getPersistence( ).withFirstMatch( PrivateAddress.named( address ), null ) { PrivateAddress privateAddress ->
      privateAddress.set( instance )
    }
  }

  @Override
  void release( String address, String ownerId ) {
    getDistinctPersistence( ).withFirstMatch( PrivateAddress.named( address ), ownerId ) { PrivateAddress privateAddress ->
      if ( EXTANT.apply( privateAddress ) || !privateAddress.assignedPartition ) {
        getPersistence( ).teardown( privateAddress )
      } else {
        privateAddress.set( null )
        privateAddress.releasing( )
      }
      void
    }
  }

  @Override
  boolean verify( String address, String ownerId ) {
    getPersistence( ).withFirstMatch( PrivateAddress.named( address ), ownerId ) { PrivateAddress privateAddress ->
      ownerId != null && ownerId == privateAddress.instanceId
    }.with{
      present ? get( ) : false
    }
  }

  @Override
  void releasing( Iterable<String> activeAddresses, String partition ) {
    getPersistence( ).withMatching( PrivateAddress.inState( RELEASING, partition ) ) { PrivateAddress privateAddress ->
      if ( !Iterables.contains( activeAddresses, privateAddress.name ) ) {
        logger.debug( "Releasing private IP address ${privateAddress.name}" )
        getPersistence( ).teardown( privateAddress )
      }
    }

    getPersistence( ).withMatching( PrivateAddress.inState( PENDING ) ) { PrivateAddress privateAddress ->
      if ( !Iterables.contains( activeAddresses, privateAddress.name ) &&
           isTimedOut( privateAddress.lastUpdateMillis( ), NetworkGroups.NETWORK_INDEX_PENDING_TIMEOUT ) ) {
        logger.warn( "Timed out pending private IP address ${privateAddress.name}" )
        getPersistence( ).teardown( privateAddress )
      }
    }
  }

  protected abstract String allocate( Iterable<Integer> addresses, Closure<String> allocator )

  protected PrivateAddressPersistence getPersistence( ){
    persistence
  }

  protected PrivateAddressPersistence getDistinctPersistence( ){
    persistence.distinct( )
  }

  @SuppressWarnings("GroovyUnusedDeclaration")
  protected final static <T,E extends Exception> T typedThrow( Class<T> type, Closure<E> closure ) throws E {
    throw closure.call( )
  }

  private static boolean isTimedOut( Long timeSinceUpdateMillis, Integer timeoutMinutes ) {
    timeSinceUpdateMillis != null &&
        timeoutMinutes != null &&
        ( timeSinceUpdateMillis > TimeUnit.MINUTES.toMillis( timeoutMinutes )  );
  }
}
