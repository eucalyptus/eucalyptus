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
import com.eucalyptus.entities.Entities
import com.google.common.base.Function
import com.google.common.base.Predicate
import com.google.common.collect.Collections2
import com.google.common.net.InetAddresses
import groovy.transform.CompileStatic
import org.apache.log4j.Logger
import org.hibernate.exception.ConstraintViolationException

import javax.annotation.Nullable
import javax.persistence.EntityTransaction

/**
 * Private address functionality
 */
@CompileStatic
class PrivateAddresses {

  private static final Logger logger = Logger.getLogger( PrivateAddress )

  static Function<String,Integer> asInteger( ) {
    AddressStringToInteger.INSTANCE
  }

  static int asInteger( final String address ) {
    InetAddresses.coerceToInteger( InetAddresses.forString( address ) )
  }

  static Function<Integer, String> fromInteger( ) {
    AddressIntegerToString.INSTANCE
  }

  static String fromInteger( final Integer address ) {
    InetAddresses.toAddrString( InetAddresses.fromInteger( address ) )
  }

  /**
   * Allocate a private address.
   *
   * <p>There must not be an active transaction for private addresses.</p>
   */
  static PrivateAddress allocate( Collection<String> addresses ) throws NotEnoughResourcesException {
    allocateI( Collections2.transform( addresses, { String address -> asInteger( address ) } as Function<String, Integer> ) )
  }

  /**
   * Allocate a private address.
   *
   * <p>There must not be an active transaction for private addresses.</p>
   */
  //TODO:STEVE: Improve private address allocation algorithm
  static PrivateAddress allocateI( Collection<Integer> addresses ) throws NotEnoughResourcesException {
    addresses.findResult{ Integer address ->
      try {
        Entities.distinctTransaction( PrivateAddress ) { EntityTransaction db ->
          PrivateAddress privateAddress = PrivateAddress.create( fromInteger( address.intValue( ) ) ).allocate( )
          Entities.persist( privateAddress )
          db.commit( )
          privateAddress
        }
      } catch ( ConstraintViolationException e ) {
        logger.trace( e )
        (PrivateAddress)null
      }
    } ?: typedThrow(PrivateAddress){ new NotEnoughResourcesException( 'Insufficient addresses' ) }
  }

  /**
   * Release a private address.
   *
   * <p>There must not be an active transaction for private addresses.</p>
   */
  static void release( String address, String ownerId ) {
    Entities.asDistinctTransaction( PrivateAddress, { PrivateAddress privateAddress ->
      Entities.query( privateAddress, Entities.queryOptions( ).build( ) )?.getAt( 0 )?.with{
        PrivateAddress entity ->
        if ( ownerId == null || entity.instanceId == ownerId ) {
          Entities.delete( entity ) //TODO:STEVE: only delete here if EXTANT, else use releasing() and clear up on custer callback
        }
      }
      true
    } as Predicate<PrivateAddress> ).apply( PrivateAddress.named( address ) )
  }

  static <T,E extends Exception> T typedThrow( Class<T> type, Closure<E> closure ) throws E {
    throw closure.call( )
  }

  private static final enum AddressIntegerToString implements Function<Integer,String> {
    INSTANCE;

    @Override
    String apply( @Nullable final Integer address ) {
      address==null ? null : fromInteger( address )
    }
  }

  private static final enum AddressStringToInteger implements Function<String,Integer> {
    INSTANCE;

    @Override
    Integer apply( @Nullable final String address ) {
      address==null ? null : asInteger( address )
    }
  }
}
