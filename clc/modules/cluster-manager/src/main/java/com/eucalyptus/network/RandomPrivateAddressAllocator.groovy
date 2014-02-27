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

import com.eucalyptus.util.Numbers
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.google.common.primitives.Floats
import com.google.common.primitives.Ints
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

/**
 * Partitioned (for laziness) random private address allocator.
 */
@CompileStatic
class RandomPrivateAddressAllocator extends PrivateAddressAllocatorSupport {

  private static final Logger logger = Logger.getLogger( FirstFreePrivateAddressAllocator )
  private static final int defaultPartitionSize =
      Ints.tryParse( System.getProperty( "com.eucalyptus.network.ramdomAllocatorPartitionSize", "" ) ) ?: 1000
  private static final float defaultPartitionAttemptFactor =
      Floats.tryParse( System.getProperty( "com.eucalyptus.network.ramdomAllocatorPartitionLoadFactor", "" ) ) ?: 1.0f
  private static final int defaultPartitionAttempts = (int)(defaultPartitionSize * defaultPartitionAttemptFactor)

  private final int partitionSize
  private final int partitionAttempts

  RandomPrivateAddressAllocator( ) {
    this( new DatabasePrivateAddressPersistence( ), defaultPartitionSize, defaultPartitionAttempts )
  }

  protected RandomPrivateAddressAllocator( final PrivateAddressPersistence persistence,
                                           final int partitionSize,
                                           final int partitionAttempts ) {
    super( logger, persistence )
    this.partitionSize = partitionSize
    this.partitionAttempts = partitionAttempts
  }

  @Override
  protected String allocate( final Iterable<Integer> addresses, final Closure<String> allocator ) {
    final Iterator<List<Integer>> addressPartitionIterator = Iterables.partition( addresses, partitionSize ).iterator( );
    while ( addressPartitionIterator.hasNext( ) ){
      final List<Integer> addressPartition = addressPartitionIterator.next( );
      final List<Integer> trimmedPartition =
          Numbers.shuffled( Lists.newArrayList( addressPartition ) )
              .subList( 0, Math.min( addressPartition.size( ), partitionAttempts ) )
      for ( Integer address : trimmedPartition ) {
        String allocated = allocator.call( address )
        if ( allocated ) return allocated;
      }
    }
    null
  }
}
