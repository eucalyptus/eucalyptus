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
package com.eucalyptus.network;

import static com.eucalyptus.compute.common.internal.util.Reference.State.*;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.compute.common.internal.util.NotEnoughResourcesException;
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.ThrowingFunction;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

/**
 *
 */
public abstract class PrivateAddressAllocatorSupport implements PrivateAddressAllocator {

  private static final String cacheSpec =
      System.getProperty( "com.eucalyptus.network.privateAddressAllocatorCacheSpec", "maximumSize=10, expireAfterWrite=30s" );

  private final LoadingCache<Pair<String, String>, Set<Integer>> cache =
      CacheBuilder.from( CacheBuilderSpec.parse( cacheSpec ) ).build( CacheLoader.from( loader( ) ) );

  private final Logger logger;
  private final PrivateAddressPersistence persistence;

  protected PrivateAddressAllocatorSupport(
      final Logger logger,
      final PrivateAddressPersistence persistence
  ) {
    this.logger = logger;
    this.persistence = persistence;
  }

  @Override
  public String allocate(
      final String scope,
      final String tag,
      final Iterable<Integer> addresses,
      final int addressCount,
      final int allocatedCount
  ) throws NotEnoughResourcesException {
    final String allocated = allocated(
        scope,
        tag,
        allocate(
            addresses,
            addressCount,
            allocatedCount,
            address -> getDistinctPersistence( )
                .tryCreate( scope, tag, PrivateAddresses.fromInteger( address.intValue( ) ) )
                .map( RestrictedTypes.toDisplayName( ) )
                .orElse( null ),
            () -> listAllocatedByScope( scope, tag ) ) );
    if ( allocated == null ) {
      throw new NotEnoughResourcesException( "Insufficient addresses" );
    }
    return allocated;
  }

  @Override
  public void associate( String address, final VmInstance instance ) throws ResourceAllocationException {
    Exceptions.unwrap( ResourceAllocationException.class, ( ) ->
      getPersistence( ).withFirstMatch(
          PrivateAddress.named( instance.getVpcId( ), address ),
          null,
          ThrowingFunction.undeclared( privateAddress -> privateAddress.set( instance ) ) ) );
  }

  @Override
  public void associate( String address, final NetworkInterface networkInterface ) throws ResourceAllocationException {
    Exceptions.unwrap( ResourceAllocationException.class, ( ) ->
      getPersistence( ).withFirstMatch(
          PrivateAddress.named( networkInterface.getVpc( ).getDisplayName( ), address ),
          null,
          ThrowingFunction.undeclared( privateAddress -> privateAddress.set( networkInterface ) ) ) );
  }

  @Override
  public String release( String scope, String address, String ownerId ) {
    final AtomicBoolean torndown = new AtomicBoolean( false );
    String tag = getDistinctPersistence( ).withFirstMatch(
        PrivateAddress.named( scope, address ),
        ownerId,
        privateAddress -> {
        if ( EXTANT.apply( privateAddress ) || Strings.isNullOrEmpty( privateAddress.getAssignedPartition( ) ) ) {
          getPersistence( ).teardown( privateAddress );
          torndown.set( true );
        } else {
          try {
            privateAddress.set( null );
            privateAddress.releasing( );
          } catch ( ResourceAllocationException e ) {
            logger.error( "Error releasing private address " + privateAddress.getDisplayName( ), e );
          }
        }
        return privateAddress.getTag( );
      } ).orElse( null );
    if ( tag != null && torndown.get( ) ) {
      released( scope, tag, address );
    }
    return tag;
  }

  @Override
  public boolean verify( String scope, String address, final String ownerId ) {
    Optional<Boolean> verified = getPersistence( ).withFirstMatch(
        PrivateAddress.named( scope, address ),
        ownerId,
        privateAddress -> ownerId != null && ownerId.equals( privateAddress.getOwnerId( ) )
    );
    return verified.orElse( false );
  }

  @Override
  public boolean releasing( final Iterable<String> activeAddresses, String partition ) {
    AtomicBoolean released = new AtomicBoolean( false );
    getPersistence( ).withMatching(
        PrivateAddress.inState( RELEASING, partition ),
        privateAddress -> {
          if ( !Iterables.contains( activeAddresses, privateAddress.getName( ) ) &&
              privateAddress.getScope( ) == null ) {
            logger.debug( "Releasing private IP address " + privateAddress.getName( ) );
            getPersistence( ).teardown( privateAddress );
            released.set( true );
        }
    } );

    getPersistence( ).withMatching(
        PrivateAddress.inState( PENDING ),
        privateAddress -> {
        if ( !Iterables.contains( activeAddresses, privateAddress.getName( ) ) &&
            isTimedOut( privateAddress.lastUpdateMillis( ), NetworkGroups.NETWORK_INDEX_PENDING_TIMEOUT ) ) {
          logger.warn( "Timed out pending private IP address " + privateAddress.getName( ) );
          getPersistence( ).teardown( privateAddress );
          released.set( true );
        }
    } );
    if ( released.get( ) ) {
      cache.invalidate( key( "", "" ) );
    }
    return released.get( );
  }

  /**
   * Allocate an address if possible.
   *
   * @param addresses      The list of all addresses
   * @param addressCount   The size of the address list
   * @param allocatedCount The number of allocated addresses, -1 if unknown
   * @param allocator      The allocator to use
   * @param lister         Supplier that can be used to list available addresses
   * @return The allocated address or null
   */
  protected abstract String allocate(
      Iterable<Integer> addresses,
      int addressCount,
      int allocatedCount,
      Function<Integer,String> allocator,
      Supplier<Set<Integer>> lister );

  protected PrivateAddressPersistence getPersistence( ) {
    return persistence;
  }

  protected PrivateAddressPersistence getDistinctPersistence( ) {
    return persistence.distinct( );
  }

  private String allocated( final String scope, final String tag, final String address ) {
    if ( address != null ) {
      final Set<Integer> allocated = cache.getIfPresent( key( scope, tag ) );
      if ( allocated != null ) {
        allocated.add( PrivateAddresses.asInteger( address ) );
      }

    }

    return address;
  }

  private String released( final String scope, final String tag, final String address ) {
    if ( address != null ) {
      final Set<Integer> allocated = cache.getIfPresent( key( scope, tag ) );
      if ( allocated != null ) {
        allocated.remove( PrivateAddresses.asInteger( address ) );
      }

    }

    return address;
  }

  private Pair<String, String> key( final String scope, final String tag ) {
    return Pair.pair( Strings.nullToEmpty( scope ), Strings.nullToEmpty( tag ) );
  }

  private static boolean isTimedOut( Long timeSinceUpdateMillis, Integer timeoutMinutes ) {
    return timeSinceUpdateMillis != null &&
        timeoutMinutes != null &&
        ( timeSinceUpdateMillis > TimeUnit.MINUTES.toMillis( timeoutMinutes ) );
  }

  private Set<Integer> listAllocatedByScope( final String scope, final String tag ) {
    return cache.getUnchecked( key( scope, tag ) );
  }

  private CompatFunction<Pair<String, String>, Set<Integer>> loader( ) {
    return new CompatFunction<Pair<String, String>, Set<Integer>>( ) {
      @Override
      public Set<Integer> apply( @Nullable final Pair<String, String> scopeAndTagKey ) {
        final Set<Integer> allocated = Collections.newSetFromMap( Maps.newConcurrentMap( ) );
        allocated.addAll( getPersistence( ).list(
            Strings.emptyToNull( scopeAndTagKey.getLeft( ) ),
            Strings.emptyToNull( scopeAndTagKey.getRight( ) ),
            pa -> PrivateAddresses.asInteger( pa.getDisplayName( ) ) ) );
        return allocated;
      }
    };
  }
}
