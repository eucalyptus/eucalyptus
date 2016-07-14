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
import com.eucalyptus.compute.common.internal.vm.VmInstance
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface as VpcNetworkInterface
import com.google.common.base.Function
import com.google.common.net.InetAddresses
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import javax.annotation.Nullable

/**
 * Private address functionality
 */
@CompileStatic
class PrivateAddresses {

  private static final PrivateAddressAllocator allocator =
      ServiceLoader.load( PrivateAddressAllocator ).iterator( ).next( )

  static Function<String,Integer> asInteger( ) {
    AddressStringToInteger.INSTANCE
  }

  static int asInteger( final String address ) {
    InetAddresses.coerceToInteger( InetAddresses.forString( address ) )
  }

  static Function<Integer, String> fromInteger( ) {
    AddressIntegerToString.INSTANCE
  }

  static String fromInteger( final Number address ) {
    InetAddresses.toAddrString( InetAddresses.fromInteger( address.intValue( ) ) )
  }

  /**
   * Allocate a private address.
   *
   * <p>There must not be an active transaction for private addresses.</p>
   */
  static String allocate(
      String scope,
      String tag,
      Iterable<Integer> addresses,
      int addressCount,
      int allocatedCount
  ) throws NotEnoughResourcesException {
    allocator.allocate( scope, tag, addresses, addressCount, allocatedCount )
  }

  static void associate( String address, VmInstance instance ) throws ResourceAllocationException {
    allocator.associate( address, instance )
  }

  static void associate( String address, VpcNetworkInterface networkInterface ) throws ResourceAllocationException {
    allocator.associate( address, networkInterface )
  }

  /**
   * Release a private address.
   *
   * <p>There must not be an active transaction for private addresses.</p>
   *
   * @return The tag for the address (if any)
   */
  static String release( String scope, String address, String ownerId ) {
    allocator.release( scope, address, ownerId )
  }

  static boolean verify( String scope, String address, String ownerId ) {
    allocator.verify( scope, address, ownerId )
  }

  @PackageScope
  static boolean releasing( Iterable<String> activeAddresses, String partition ) {
    allocator.releasing( activeAddresses, partition )
  }

  protected PrivateAddressAllocator allocator( ) {
    allocator
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
