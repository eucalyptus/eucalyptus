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

import com.eucalyptus.entities.Entities
import com.google.common.base.Function
import com.google.common.base.Optional
import groovy.transform.CompileStatic
import org.apache.log4j.Logger
import org.hibernate.exception.ConstraintViolationException

import javax.persistence.EntityTransaction

/**
 *
 */
@CompileStatic
class DatabasePrivateAddressPersistence implements PrivateAddressPersistence {

  private final Logger logger = Logger.getLogger( DatabasePrivateAddressPersistence )

  @Override
  Optional<PrivateAddress> tryCreate( final String address ) {
    try {
      transaction( PrivateAddress ) { EntityTransaction db ->
        PrivateAddress privateAddress = PrivateAddress.create( address ).allocate( )
        Entities.persist( privateAddress )
        db.commit( )
        Optional.of( privateAddress )
      }
    } catch ( ConstraintViolationException e ) {
      logger.trace( e )
      Optional.absent( )
    }
  }

  @Override
  void teardown( final PrivateAddress address ) {
    address.teardown( )
  }

  @Override
  def <V> Optional<V> withFirstMatch( final PrivateAddress address,
                                      final String ownerId,
                                      final Closure<V> closure ) {
    asTransaction( PrivateAddress, { PrivateAddress privateAddress ->
      Entities.query( privateAddress, Entities.queryOptions( ).build( ) )?.getAt( 0 )?.with{
        PrivateAddress entity ->
          if ( ownerId == null || entity.instanceId == ownerId ) {
            Optional.fromNullable( closure.call( entity ) )
          } else {
            Optional.absent( )
          }
      } ?: Optional.absent( )
    } as Function<PrivateAddress, Optional<V>> ).apply( address )
  }

  @Override
  void withMatching( final PrivateAddress address,
                     final Closure<?> closure ) {
    transaction( PrivateAddress ) { EntityTransaction db ->
      Entities.query( address, Entities.queryOptions( ).build( ) ).each{
        PrivateAddress entity -> closure.call( entity )
      }
      db.commit( )
    }
  }

  @Override
  PrivateAddressPersistence distinct( ) {
    new DistinctDatabasePrivateAddressPersistence( )
  }

  protected <V> V transaction( final Object obj, final Closure<V> closure ) {
    Entities.transaction( obj, closure )
  }

  protected <F,T> Function<F,T> asTransaction( final Class<?> clazz, final Function<F,T> function ) {
    Entities.asTransaction( clazz, function )
  }

  private static class DistinctDatabasePrivateAddressPersistence extends DatabasePrivateAddressPersistence {
    @Override
    PrivateAddressPersistence distinct() {
      this
    }

    @Override
    protected <V> V transaction(final Object obj,final Closure<V> closure) {
      Entities.distinctTransaction( obj, closure )
    }

    @Override
    protected <F, T> Function<F, T> asTransaction(final Class<?> clazz,final Function<F, T> function) {
      Entities.asDistinctTransaction( clazz, function )
    }
  }
}
