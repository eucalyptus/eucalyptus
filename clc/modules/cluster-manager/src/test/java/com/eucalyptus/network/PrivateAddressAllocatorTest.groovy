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

import com.eucalyptus.compute.common.internal.vm.VmInstance
import com.google.common.base.Function
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
    PrivateAddressAllocator allocator = new RandomPrivateAddressAllocator( persistence, 10, 1, 3, 10 )
    verifyBasicAllocation( persistence.reset( ), allocator )
    verifyAddressesExhaustedFailure( persistence.reset( ), allocator )
    verifyHugeAddressesExhaustedFailure( persistence.reset( ), allocator )
    verifyEarlyRelease( persistence.reset( ), allocator )
    verifyLazyAllocation( persistence.reset( ), allocator )
    verifyFullRange( persistence.reset( ), allocator )
    verifyOneAvailableAddress( persistence.reset( ), new RandomPrivateAddressAllocator( persistence, 1000, 15, 3, 10 ) )
  }

  @Test
  void testFirstFreeAllocator( ) {
    TestPrivateAddressPersistence persistence = new TestPrivateAddressPersistence( )
    PrivateAddressAllocator allocator = new FirstFreePrivateAddressAllocator( persistence )
    verifyBasicAllocation( persistence.reset( ), allocator )
    verifyAddressesExhaustedFailure( persistence.reset( ), allocator )
    verifyHugeAddressesExhaustedFailure( persistence.reset( ), allocator )
    verifyEarlyRelease( persistence.reset( ), allocator )
    verifyLazyAllocation( persistence.reset( ), allocator )
    verifyFullRange( persistence.reset( ), allocator )
    verifyOneAvailableAddress( persistence.reset( ), allocator )
  }

  private void verifyBasicAllocation( TestPrivateAddressPersistence persistence,
                                      PrivateAddressAllocator allocator ) {
    String address = allocator.allocate( null, null, ranges( '10.0.0.0-10.0.0.10' ), 10, 0 )
    assertTrue( 'one address allocated', persistence.addresses.size( ) == 1 );
    allocator.associate( address, instance( ) )
    assertTrue( 'address ownership can be verified', allocator.verify( null, address, 'i-12345678' ) )
    allocator.release( null, address, 'i-12345678' )
    assertTrue( 'no addresses allocated', persistence.addresses.size() == 0 );
  }

  private void verifyAddressesExhaustedFailure( TestPrivateAddressPersistence persistence,
                                                PrivateAddressAllocator allocator ) {
    allocator.allocate( null, null, ranges( '10.0.0.0' ), 1 , 0)
    assertTrue( 'one address allocated', persistence.addresses.size( ) == 1 );
    try {
      allocator.allocate( null, null, ranges( '10.0.0.0' ), 1, 1 )
      fail( 'Allocation should have failed due to no available addresses' )
    } catch ( NotEnoughResourcesException ) {
      assertTrue( 'one address allocated', persistence.addresses.size( ) == 1 );
    }
  }

  /**
   * Provides coverage for failed allocation when a large number of address are advertised
   */
  private void verifyHugeAddressesExhaustedFailure( TestPrivateAddressPersistence persistence,
                                                    PrivateAddressAllocator allocator ) {
    allocator.allocate( null, null, ranges( '10.0.0.0' ), 1_000_000 , 0)
    assertTrue( 'one address allocated', persistence.addresses.size( ) == 1 );
    try {
      allocator.allocate( null, null, ranges( '10.0.0.0' ), 1_000_000, 1 )
      fail( 'Allocation should have failed due to no available addresses' )
    } catch ( NotEnoughResourcesException ) {
      assertTrue( 'one address allocated', persistence.addresses.size( ) == 1 );
    }
  }

  private void verifyEarlyRelease( TestPrivateAddressPersistence persistence,
                                   PrivateAddressAllocator allocator ) {
    String address = allocator.allocate( null, null, ranges( '10.0.0.0-10.0.0.10' ), 10, 0 )
    assertTrue( 'one address allocated', persistence.addresses.size( ) == 1 );
    allocator.release( null, address, null )
    assertTrue( 'no address allocated', persistence.addresses.isEmpty( ) );
  }

  private void verifyLazyAllocation( TestPrivateAddressPersistence persistence,
                                     PrivateAddressAllocator allocator ) {
    allocator.allocate( null, null, Iterables.concat( [ ranges( '10.0.0.0-10.0.0.10' ), new Iterable<Integer>(){
      @Override
      Iterator<Integer> iterator() {
        fail( 'iterator should not be required for allocation' )
        null
      }
    } ] as List<Iterable<Integer>>), 11, 0 )
    assertTrue( 'one address allocated', persistence.addresses.size( ) == 1 );
  }

  private void verifyFullRange( TestPrivateAddressPersistence persistence,
                                PrivateAddressAllocator allocator ) {
    ( 1..100 ).each{ allocator.allocate( null, null, ranges( '0.0.0.0-255.255.255.255' ), Integer.MAX_VALUE, 0 ) }
    assertTrue( '100 addresses allocated', persistence.addresses.size( ) == 100 );
  }

  private void verifyOneAvailableAddress( TestPrivateAddressPersistence persistence,
                                          PrivateAddressAllocator allocator ) {
    Iterable<Integer> ranges = ranges( '10.0.0.0-10.0.0.255' )
    assertEquals( 'Expected 256 addresses', 256, Iterables.size( ranges ) )
    Iterator<Integer> rangeIterator = ranges.iterator( )
    (1..255).each{
      String address =  PrivateAddresses.fromInteger( rangeIterator.next( ) )
      persistence.addresses.put( address, PrivateAddress.create( null, null, address ).allocate( ) )
    }
    assertEquals( '255 addresses allocated', 255, persistence.addresses.size( ) );
    String address = allocator.allocate( null, null, ranges, 256, 255 )
    assertNotNull( 'Expected address', address )
    assertEquals( '256 addresses allocated', 256, persistence.addresses.size( ) );
    allocator.release( null, address, null )
    assertEquals( '255 addresses allocated', 255, persistence.addresses.size( ) );
    String address2 = allocator.allocate( null, null, ranges, 256, -1 )
    assertNotNull( 'Expected address', address2 )
    assertEquals( '256 addresses allocated', 256, persistence.addresses.size( ) );
  }

  private VmInstance instance( ) {
    new VmInstance( null, 'i-12345678' ) {
      @Override String getPartition( ) {  'PARTI00' }
      @Override String getVpcId( ) { null }
    }
  }

  private Iterable<Integer> ranges( String... ranges ) {
    Iterables.concat( ranges.collect{ String range -> IPRange.parse( range ) } )
  }

  static class TestPrivateAddressPersistence implements PrivateAddressPersistence {
    final Map<String,PrivateAddress> addresses = Maps.newHashMapWithExpectedSize( 20000 )

    @Override
    Optional<PrivateAddress> tryCreate( final String scope, final String tag, final String address ) {
      addresses.containsKey( address ) ?
          Optional.absent( ) :
          Optional.of( add( PrivateAddress.create( scope, tag, address ).allocate( ) ) )
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

    @Override
    def <T> List<T> list( final String scope,
                          final String tag,
                          final Function<PrivateAddress, T> transform ) {
      addresses.values()
          .findAll{ PrivateAddress pa -> pa.scope == scope && pa.tag == tag }
          .collect{ PrivateAddress pa -> transform.apply( pa ) } as List<T>
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
