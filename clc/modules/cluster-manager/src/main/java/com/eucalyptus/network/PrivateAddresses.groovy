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
