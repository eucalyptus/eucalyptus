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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.apache.log4j.Logger;
import com.eucalyptus.util.Numbers;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;

/**
 * Partitioned (for laziness) random private address allocator.
 */
public class RandomPrivateAddressAllocator extends PrivateAddressAllocatorSupport {

  private static final Logger logger = Logger.getLogger( RandomPrivateAddressAllocator.class );
  private static final int defaultPartitionSize =
      MoreObjects.firstNonNull( Ints.tryParse( System.getProperty( "com.eucalyptus.network.ramdomAllocatorPartitionSize", "" ) ), 1000 );
  private static final int defaultPartitionCount =
      MoreObjects.firstNonNull( Ints.tryParse( System.getProperty( "com.eucalyptus.network.ramdomAllocatorPartitionCount", "" ) ), 15 );
  private static final int defaultListingFailureThreshold =
      MoreObjects.firstNonNull( Ints.tryParse( System.getProperty( "com.eucalyptus.network.ramdomAllocatorListingFailureThreshold", "" ) ), 3 );
  private static final float defaultPartitionAttemptFactor =
      MoreObjects.firstNonNull( Floats.tryParse( System.getProperty( "com.eucalyptus.network.ramdomAllocatorPartitionLoadFactor", "" ) ), 0.01f );
  private static final int defaultPartitionAttempts = (int)(defaultPartitionSize * defaultPartitionAttemptFactor);

  public enum ListPrivateAddresses{ Never, OnFailure, Always }

  private final int partitionSize;
  private final int partitionCount;
  private final int listingFailureThreshold;
  private final int partitionAttempts;

  public RandomPrivateAddressAllocator( ) {
    this(
        new DatabasePrivateAddressPersistence( ),
        defaultPartitionSize,
        defaultPartitionCount,
        defaultListingFailureThreshold,
        defaultPartitionAttempts );
  }

  protected RandomPrivateAddressAllocator(
      final PrivateAddressPersistence persistence,
      final int partitionSize,
      final int partitionCount,
      final int listingFailureThreshold,
      final int partitionAttempts
  ) {
    super( logger, persistence );
    this.partitionSize = partitionSize;
    this.partitionCount = partitionCount;
    this.listingFailureThreshold = listingFailureThreshold;
    this.partitionAttempts = partitionAttempts;
  }

  @Override
  protected String allocate(
      final Iterable<Integer> addresses,
      final int addressCount,
      final int allocatedCount,
      final Function<Integer,String> allocator,
      final Supplier<Set<Integer>> lister
  ) {
    if ( addressCount < ( partitionSize * partitionCount ) ) {
      ListPrivateAddresses listAddresses = allocatedCount > ( addressCount / 4 ) ? ListPrivateAddresses.Always : ListPrivateAddresses.OnFailure;
      return doAllocate( listAddresses, addresses, partitionSize, allocator, lister );
    } else {
      return doAllocate( ListPrivateAddresses.Never, addresses, partitionAttempts, allocator, lister );
    }
  }

  private String doAllocate(
      final ListPrivateAddresses listAddresses,
      final Iterable<Integer> addresses,
      final int attempts,
      final Function<Integer,String> allocator,
      final Supplier<Set<Integer>> lister
  ) {
    String allocated;
    if ( listAddresses.equals( ListPrivateAddresses.Always ) ) {
      allocated = doAllocateWithListing( addresses, allocator, lister );
    } else {
      final AtomicInteger allocationFailures = new AtomicInteger( 0 );
      allocated = visitPartitioned( addresses, attempts, allocator, () -> {
          if ( ListPrivateAddresses.OnFailure == listAddresses &&
              allocationFailures.incrementAndGet( ) >= listingFailureThreshold ) {
            return doAllocateWithListing( addresses, allocator, lister );
          }
          return null;
      } );
    }
    return allocated;
  }

  private String doAllocateWithListing(
      final Iterable<Integer> allAddresses,
      final Function<Integer,String> allocator,
      final Supplier<Set<Integer>> lister
  ) {
    while ( true ) {
      Iterable<Integer> addresses = Iterables.filter( allAddresses, Predicates.not( Predicates.in( lister.get( ) ) ) );
      if ( Iterables.isEmpty( addresses ) ) return null;
      String allocated = visitPartitioned( addresses, partitionAttempts, allocator, Suppliers.ofInstance( null ) );
      if ( allocated != null ) return allocated;
    }
  }

  private String visitPartitioned(
      final Iterable<Integer> addresses,
      final int attempts,
      final Function<Integer,String> allocator,
      final Supplier<String> failureHandler
  ) {
    final Iterator<List<Integer>> addressPartitionIterator = Iterables.partition( addresses, partitionSize ).iterator( );
    while ( addressPartitionIterator.hasNext( ) ) {
      final List<List<Integer>> partitions = Lists.newArrayList( Iterators.limit( addressPartitionIterator, partitionCount ) );
      Collections.shuffle( partitions );
      for ( final List<Integer> addressPartition : partitions ) {
        final List<Integer> trimmedPartition = Numbers.shuffled( Lists.newArrayList( addressPartition ) ).subList( 0, Math.min( addressPartition.size( ), attempts ) );
        for ( Integer address : trimmedPartition ) {
          String allocated = allocator.apply( address );
          if ( allocated != null ) {
            return allocated;
          } else {
            allocated = failureHandler.get( );
            if ( allocated != null ) {
              return allocated;
            }
          }
        }
      }
    }
    return null;
  }
}
