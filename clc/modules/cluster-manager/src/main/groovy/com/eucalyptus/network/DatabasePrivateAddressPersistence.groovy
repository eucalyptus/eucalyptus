/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.network

import com.eucalyptus.entities.Entities
import com.google.common.base.Function
import com.google.common.base.Optional
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
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
  Optional<PrivateAddress> tryCreate( final String scope, final String tag, final String address ) {
    try {
      transaction( PrivateAddress ) { EntityTransaction db ->
        PrivateAddress privateAddress = PrivateAddress.create( scope, tag, address ).allocate( )
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
      Entities.query( privateAddress )?.getAt( 0 )?.with{
        PrivateAddress entity ->
          if ( ownerId == null || entity.ownerId == ownerId ) {
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
  def <T> List<T> list( final String scope,
                        final String tag,
                        final Function<PrivateAddress, T> transform ) {
    transaction( PrivateAddress ) { EntityTransaction db ->
      Lists.newArrayList( Iterables.transform( Entities.query( PrivateAddress.scoped( scope, tag ) ), transform ) )
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
