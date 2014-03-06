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

import com.eucalyptus.vm.VmInstance
import com.google.common.base.Optional
import com.google.common.collect.Iterables
import com.google.common.collect.Maps
import groovy.transform.CompileStatic
import org.junit.Test
import static org.junit.Assert.*

/**
 *
 */
@CompileStatic
class PrivateAddressAllocatorTest {

  @Test
  void testRandomAllocator( ) {
    TestPrivateAddressPersistence persistence = new TestPrivateAddressPersistence( )
    PrivateAddressAllocator allocator = new RandomPrivateAddressAllocator( persistence, 10, 10 )
    verifyBasicAllocation( persistence.reset( ), allocator )
    verifyAddressesExhaustedFailure( persistence.reset( ), allocator )
    verifyEarlyRelease( persistence.reset( ), allocator )
    verifyLazyAllocation( persistence.reset( ), allocator )
    verifyFullRange( persistence.reset( ), allocator )
  }

  @Test
  void testFirstFreeAllocator( ) {
    TestPrivateAddressPersistence persistence = new TestPrivateAddressPersistence( )
    PrivateAddressAllocator allocator = new FirstFreePrivateAddressAllocator( persistence )
    verifyBasicAllocation( persistence.reset( ), allocator )
    verifyAddressesExhaustedFailure( persistence.reset( ), allocator )
    verifyEarlyRelease( persistence.reset( ), allocator )
    verifyLazyAllocation( persistence.reset( ), allocator )
    verifyFullRange( persistence.reset( ), allocator )
  }

  private void verifyBasicAllocation( TestPrivateAddressPersistence persistence,
                                      PrivateAddressAllocator allocator ) {
    String address = allocator.allocate( ranges( '10.0.0.0-10.0.0.10' ) )
    assertTrue( 'one address allocated', persistence.addresses.size( ) == 1 );
    allocator.associate( address, instance( ) )
    assertTrue( 'address ownership can be verified', allocator.verify( address, 'i-12345678' ) )
    allocator.release( address, 'i-12345678' )
    assertTrue( 'no addresses allocated', persistence.addresses.size() == 0 );
  }

  private void verifyAddressesExhaustedFailure( TestPrivateAddressPersistence persistence,
                                                PrivateAddressAllocator allocator ) {
    allocator.allocate( ranges( '10.0.0.0' ) )
    assertTrue( 'one address allocated', persistence.addresses.size( ) == 1 );
    try {
      allocator.allocate( ranges( '10.0.0.0' ) )
      fail( 'Allocation should have failed due to no available addresses' )
    } catch ( NotEnoughResourcesException ) {
      assertTrue( 'one address allocated', persistence.addresses.size( ) == 1 );
    }
  }

  private void verifyEarlyRelease( TestPrivateAddressPersistence persistence,
                                   PrivateAddressAllocator allocator ) {
    String address = allocator.allocate( ranges( '10.0.0.0-10.0.0.10' ) )
    assertTrue( 'one address allocated', persistence.addresses.size( ) == 1 );
    allocator.release( address, null )
    assertTrue( 'no address allocated', persistence.addresses.isEmpty( ) );
  }

  private void verifyLazyAllocation( TestPrivateAddressPersistence persistence,
                                     PrivateAddressAllocator allocator ) {
    allocator.allocate( Iterables.concat( [ ranges( '10.0.0.0-10.0.0.10' ), new Iterable<Integer>(){
      @Override
      Iterator<Integer> iterator() {
        fail( 'iterator should not be required for allocation' )
        null
      }
    } ] as List<Iterable<Integer>>) )
    assertTrue( 'one address allocated', persistence.addresses.size( ) == 1 );
  }

  private void verifyFullRange( TestPrivateAddressPersistence persistence,
                                PrivateAddressAllocator allocator ) {
    ( 1..100 ).each{ allocator.allocate( ranges( '0.0.0.0-255.255.255.255' ) ) }
    assertTrue( 'one address allocated', persistence.addresses.size( ) == 100 );
  }

  private VmInstance instance( ) {
    new VmInstance( null, 'i-12345678' ) {
      @Override
      String getPartition() {
        return 'PARTI00'
      }
    }
  }

  private Iterable<Integer> ranges( String... ranges ) {
    Iterables.concat( ranges.collect{ String range -> IPRange.parse( range ) } )
  }

  static class TestPrivateAddressPersistence implements PrivateAddressPersistence {
    final Map<String,PrivateAddress> addresses = Maps.newHashMapWithExpectedSize( 20000 )

    @Override
    Optional<PrivateAddress> tryCreate( final String address ) {
      addresses.containsKey( address ) ?
          Optional.absent( ) :
          Optional.of( add( PrivateAddress.create( address ).allocate( ) ) )
    }

    @Override
    void teardown( final PrivateAddress address ) {
      addresses.remove( address.name )
    }

    @Override
    def <V> Optional<V> withFirstMatch( final PrivateAddress address,
                                        final String ownerId,
                                        final Closure<V> closure ) {
      addresses.get( address.name )?.with{ PrivateAddress pa ->
        Optional.fromNullable( closure.call( pa ) )
      } ?: Optional.absent( )
    }

    @Override
    void withMatching( final PrivateAddress address,
                       final Closure<?> closure ) {
      address.name ?
        addresses.values().findAll{ PrivateAddress pa -> pa.name == address.name }.each( closure ) :
        addresses.values().findAll{ PrivateAddress pa -> pa.state == address.state }.each( closure )
    }

    private PrivateAddress add( PrivateAddress address ) {
      addresses.put( (String)address.name, address )
      address
    }

    TestPrivateAddressPersistence distinct( ) {
      this
    }

    TestPrivateAddressPersistence reset( ) {
      addresses.clear( )
      this
    }
  }
}
