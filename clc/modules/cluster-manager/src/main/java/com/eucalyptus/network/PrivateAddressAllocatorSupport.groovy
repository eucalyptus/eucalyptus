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
package com.eucalyptus.network

import com.eucalyptus.compute.common.internal.util.NotEnoughResourcesException
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException
import com.eucalyptus.util.Pair
import com.eucalyptus.util.RestrictedTypes
import com.eucalyptus.compute.common.internal.vm.VmInstance
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface as VpcNetworkInterface
import com.google.common.base.Function
import com.google.common.base.Strings
import com.google.common.base.Supplier
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheBuilderSpec
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.collect.Iterables
import com.google.common.collect.Maps
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import java.util.concurrent.TimeUnit

import static com.eucalyptus.compute.common.internal.util.Reference.State.*

/**
 *
 */
@CompileStatic
abstract class PrivateAddressAllocatorSupport implements PrivateAddressAllocator {

  private static final String cacheSpec =
      System.getProperty( "com.eucalyptus.network.privateAddressAllocatorCacheSpec", "maximumSize=10, expireAfterWrite=30s" )

  private final LoadingCache<Pair<String, String>,Set<Integer>> cache =
      CacheBuilder.from( CacheBuilderSpec.parse( cacheSpec ) ).build( CacheLoader.from( loader( ) ) );

  private final Logger logger
  private final PrivateAddressPersistence persistence

  protected PrivateAddressAllocatorSupport( final Logger logger,
                                            final PrivateAddressPersistence persistence ) {
    this.logger = logger
    this.persistence = persistence
  }

  @Override
  String allocate( String scope, String tag, Iterable<Integer> addresses, int addressCount, int allocatedCount ) throws NotEnoughResourcesException {
    allocated( scope, tag, allocate( addresses, addressCount, allocatedCount, { Integer address ->
      getDistinctPersistence( ).tryCreate( scope, tag, PrivateAddresses.fromInteger( address.intValue( ) ) )
          .transform( RestrictedTypes.toDisplayName( ) ).orNull( )
    } as Closure<String>, { listAllocatedByScope( scope, tag ) } as Supplier<Set<Integer>> ) ) ?:
        typedThrow(String){ new NotEnoughResourcesException( 'Insufficient addresses' ) }
  }

  @Override
  void associate( String address, VmInstance instance ) throws ResourceAllocationException {
    getPersistence( ).withFirstMatch( PrivateAddress.named( instance.getVpcId( ), address ), null ) { PrivateAddress privateAddress ->
      privateAddress.set( instance )
    }
  }

  @Override
  void associate( String address, VpcNetworkInterface networkInterface ) throws ResourceAllocationException {
    getPersistence( ).withFirstMatch( PrivateAddress.named( networkInterface.getVpc( ).getDisplayName( ), address ), null ) { PrivateAddress privateAddress ->
      privateAddress.set( networkInterface )
    }
  }

  @Override
  String release( String scope, String address, String ownerId ) {
    boolean torndown = false
    String tag = getDistinctPersistence( ).withFirstMatch( PrivateAddress.named( scope, address ), ownerId ) { PrivateAddress privateAddress ->
      if ( EXTANT.apply( privateAddress ) || !privateAddress.assignedPartition ) {
        getPersistence( ).teardown( privateAddress )
        torndown = true
      } else {
        privateAddress.set( null )
        privateAddress.releasing( )
      }
      privateAddress.tag
    }.orNull( )
    if ( tag && torndown ) released( scope, tag, address )
    tag
  }

  @Override
  boolean verify( String scope, String address, String ownerId ) {
    getPersistence( ).withFirstMatch( PrivateAddress.named( scope, address ), ownerId ) { PrivateAddress privateAddress ->
      ownerId != null && ownerId == privateAddress.ownerId
    }.with{
      present ? get( ) : false
    }
  }

  @Override
  boolean releasing( Iterable<String> activeAddresses, String partition ) {
    boolean released = false
    getPersistence( ).withMatching( PrivateAddress.inState( RELEASING, partition ) ) { PrivateAddress privateAddress ->
      if ( !Iterables.contains( activeAddresses, privateAddress.name ) && privateAddress.getScope( ) == null ) {
        logger.debug( "Releasing private IP address ${privateAddress.name}" )
        getPersistence( ).teardown( privateAddress )
        released = true
      }
      void
    }

    getPersistence( ).withMatching( PrivateAddress.inState( PENDING ) ) { PrivateAddress privateAddress ->
      if ( !Iterables.contains( activeAddresses, privateAddress.name ) &&
           isTimedOut( privateAddress.lastUpdateMillis( ), NetworkGroups.NETWORK_INDEX_PENDING_TIMEOUT ) ) {
        logger.warn( "Timed out pending private IP address ${privateAddress.name}" )
        getPersistence( ).teardown( privateAddress )
        released = true
      }
      void
    }
    if ( released ) cache.invalidate( key( '', '') )
    released
  }

  /**
   * Allocate an address if possible.
   *
   * @param addresses The list of all addresses
   * @param addressCount The size of the address list
   * @param allocatedCount The number of allocated addresses, -1 if unknown
   * @param allocator The allocator to use
   * @param lister Supplier that can be used to list available addresses
   * @return The allocated address or null
   */
  protected abstract String allocate(
      Iterable<Integer> addresses,
      int addressCount,
      int allocatedCount,
      Closure<String> allocator,
      Supplier<Set<Integer>> lister
      )

  protected PrivateAddressPersistence getPersistence( ){
    persistence
  }

  protected PrivateAddressPersistence getDistinctPersistence( ){
    persistence.distinct( )
  }

  private String allocated(
      final String scope,
      final String tag,
      final String address
  ) {
    if ( address ) {
      final Set<Integer> allocated = cache.getIfPresent( key( scope, tag ) )
      if ( allocated != null ) {
        allocated.add( PrivateAddresses.asInteger( address ) )
      }
    }
    address
  }

  private String released(
      final String scope,
      final String tag,
      final String address
  ) {
    if ( address ) {
      final Set<Integer> allocated = cache.getIfPresent( key( scope, tag ) )
      if ( allocated != null ) {
        allocated.remove( PrivateAddresses.asInteger( address ) )
      }
    }
    address
  }

  private Pair<String,String> key( final String scope, final String tag ) {
    Pair.pair(Strings.nullToEmpty( scope ), Strings.nullToEmpty( tag ) )
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

  private Set<Integer> listAllocatedByScope( final String scope, final String tag ) {
    cache.getUnchecked( key( scope, tag ) )
  }

  private Function<Pair<String,String>,Set<Integer>> loader( ) {
    { Pair<String,String> scopeAndTagKey ->
      Set<Integer> allocated = Collections.newSetFromMap( Maps.<Integer,Boolean>newConcurrentMap( ) )
      allocated.addAll( getPersistence( ).list(
          Strings.emptyToNull( scopeAndTagKey.left ),
          Strings.emptyToNull( scopeAndTagKey.right ),
          { PrivateAddress pa -> PrivateAddresses.asInteger( pa.getDisplayName( ) ) } as Function<PrivateAddress,Integer>
      ) )
      allocated
    } as Function<Pair<String,String>,Set<Integer>>
  }
}
