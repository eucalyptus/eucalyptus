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
package com.eucalyptus.network;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.CompatFunction;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 *
 */
public class DatabasePrivateAddressPersistence implements PrivateAddressPersistence {

  @Override
  public Optional<PrivateAddress> tryCreate( final String scope, final String tag, final String address ) {
    try {
      return transaction( PrivateAddress.class, db -> {
        final PrivateAddress privateAddress;
        try {
          privateAddress = PrivateAddress.create( scope, tag, address ).allocate( );
        } catch ( ResourceAllocationException e ) {
          return Optional.empty( );
        }
        Entities.persist( privateAddress );
        db.commit( );
        return Optional.of( privateAddress );
      } );
    } catch ( ConstraintViolationException e ) {
      logger.trace( e );
      return Optional.empty( );
    }
  }

  @Override
  public void teardown( final PrivateAddress address ) {
    try {
      address.teardown( );
    } catch ( ResourceAllocationException e ) {
      logger.trace( e );
    }
  }

  @Override
  public <V> Optional<V> withFirstMatch( final PrivateAddress address,
                                         final String ownerId,
                                         final Function<? super PrivateAddress, V> transform ) {
    return asTransaction( PrivateAddress.class, (PrivateAddress privateAddress) -> {
      final List<PrivateAddress> addresses = Entities.query( privateAddress );
      if ( !addresses.isEmpty( ) ) {
        final PrivateAddress entity = addresses.get( 0 );
        if ( ownerId == null || ownerId.equals( entity.getOwnerId( ) ) ) {
          return Optional.ofNullable( transform.apply( entity ) );
        } else {
          return Optional.<V>empty( );
        }
      } else {
        return Optional.<V>empty( );
      }
    } ).apply( address );
  }

  @Override
  public void withMatching( final PrivateAddress address, final Callback<? super PrivateAddress> callback ) {
    transaction( PrivateAddress.class, db -> {
        for ( final PrivateAddress entity : Entities.query( address, Entities.queryOptions( ).build( ) ) ) {
          callback.fire( entity );
        }
        db.commit( );
        return null;
    } );
  }

  @Override
  public <T> List<T> list( final String scope, final String tag, final Function<? super PrivateAddress, T> transform ) {
    return transaction( PrivateAddress.class, db ->
        Lists.newArrayList( Iterables.transform(
            Entities.query( PrivateAddress.scoped( scope, tag ) ),
            CompatFunction.of( transform ) ) )
    );
  }

  @Override
  public PrivateAddressPersistence distinct( ) {
    return new DistinctDatabasePrivateAddressPersistence( );
  }

  protected <V> V transaction( final Object obj, final Function<? super TransactionResource,? extends V> callback ) {
    try ( final TransactionResource tx = Entities.transactionFor( obj ) ) {
      return callback.apply( tx );
    }
  }

  protected <F, T> Function<F, T> asTransaction( final Class<?> clazz, final Function<F, T> function ) {
    return CompatFunction.of( Entities.asTransaction( clazz, CompatFunction.of( function ) ) );
  }

  private final Logger logger = Logger.getLogger( DatabasePrivateAddressPersistence.class );

  private static class DistinctDatabasePrivateAddressPersistence extends DatabasePrivateAddressPersistence {

    @Override
    public PrivateAddressPersistence distinct( ) {
      return this;
    }

  }
}
