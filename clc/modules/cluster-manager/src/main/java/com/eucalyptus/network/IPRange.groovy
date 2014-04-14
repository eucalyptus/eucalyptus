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

import com.eucalyptus.util.Parameters
import com.google.common.base.Function
import com.google.common.base.Optional
import com.google.common.base.Splitter
import com.google.common.collect.AbstractSequentialIterator
import com.google.common.collect.Iterators
import com.google.common.net.InetAddresses
import com.google.common.primitives.UnsignedInteger
import com.google.common.primitives.UnsignedInts
import groovy.transform.CompileStatic
import groovy.transform.Immutable

import javax.annotation.Nullable

import static org.hamcrest.Matchers.notNullValue

/**
 *
 */
@CompileStatic
@Immutable
class IPRange implements Iterable<Integer> {
  int lower;
  int upper;

  /**
   * Parse an ip range, which may be a single value or a range.
   *
   * @throws IllegalArgumentException if the given range is invalid
   */
  static IPRange parse( String range ) {
    Parameters.checkParam( "range", range, notNullValue( ) )
    final Iterable<String> parts = Splitter.on('-').trimResults().omitEmptyStrings().limit(2).split( range )
    final String lowerPart = parts.getAt( 0 )
    if ( lowerPart == null ) {
      throw new IllegalArgumentException( "Invalid range: ${range}" )
    }
    final String upperPart = parts.getAt( 1 )?:lowerPart
    if ( !InetAddresses.isInetAddress( lowerPart ) || !InetAddresses.isInetAddress( upperPart ) ) {
      throw new IllegalArgumentException( "Invalid range: ${range}" )
    }
    final int lower = PrivateAddresses.asInteger( lowerPart )
    final int upper = PrivateAddresses.asInteger( upperPart )
    if ( UnsignedInts.compare( lower, upper ) > 0 ) {
      throw new IllegalArgumentException( "Invalid range: ${range}" )
    }
    new IPRange( lower, upper )
  }

  static Function<String,Optional<IPRange>> parse( ) {
    IPRangeParse.INSTANCE
  }

  /**
   * Create an ip range for the given subnet and netmask
   *
   * @throws IllegalArgumentException if the given subnet or netmask is invalid
   */
  static IPRange fromSubnet( String subnet, String netmask ) {
    Parameters.checkParam( "subnet", subnet, notNullValue( ) )
    Parameters.checkParam( "netmask", netmask, notNullValue( ) )
    if ( !InetAddresses.isInetAddress( subnet ) || !InetAddresses.isInetAddress( netmask ) ) {
      throw new IllegalArgumentException( "Invalid subnet/netmask: ${subnet}/${netmask}" )
    }
    final int snet = PrivateAddresses.asInteger( subnet )
    final int mask = PrivateAddresses.asInteger( netmask )
    new IPRange( snet & mask, snet | ( -1 ^ mask ) ).perhapsShrink( )
  }

  static boolean isIPRange( String range ) {
    parse( ).apply( range ).present
  }

  /**
   * Split this range around ip, omitting ip from the resulting ranges.
   */
  List<IPRange> split( String ip ) {
    Parameters.checkParam( "ip", ip, notNullValue( ) )
    if ( !InetAddresses.isInetAddress( ip ) ) {
      throw new IllegalArgumentException( "Invalid ip: ${ip}" )
    }
    final int address =  PrivateAddresses.asInteger( ip )
    if ( UnsignedInts.compare( address, lower ) > 0 && UnsignedInts.compare( address, upper ) < 0 ) {
      [
          new IPRange( lower, unsigned(address).minus(unsigned(1)).intValue() ),
          new IPRange( unsigned(address).plus(unsigned(1)).intValue(), upper )
      ]
    } else if ( UnsignedInts.compare( lower, address ) == 0 ) {
      [ new IPRange( unsigned(address).plus(unsigned(1)).intValue(), upper ) ]
    } else if ( UnsignedInts.compare( upper, address ) == 0 ) {
      [ new IPRange( lower, unsigned(address).minus(unsigned(1)).intValue() ) ]
    } else {
      [ this ]
    }
  }

  /**
   * Does this range (inclusively) contain the given range.
   *
   * @param range The range to test
   */
  boolean contains( IPRange range ) {
    UnsignedInts.compare( lower, range.lower ) < 1 && UnsignedInts.compare( upper, range.upper ) > -1
  }

  /**
   * Shrink this range if possible by one at each end.
   *
   * <p>Useful to skip the network address and broadcast address in a subnet.</p>
   */
  IPRange perhapsShrink( ) {
    UnsignedInteger shrinkLower = unsigned( lower ).plus( unsigned( 1 ) )
    UnsignedInteger shrinkUpper = unsigned( upper ).minus( unsigned( 1 ) )
    shrinkLower.compareTo( shrinkUpper ) <= 0 ?
        new IPRange( shrinkLower.intValue( ), shrinkUpper.intValue( ) ) :
        this
  }

  @Override
  Iterator<Integer> iterator( ) {
    Iterators.<Long,Integer>transform(
        new LongSequentialIterator( lower, upper ).iterator( ),
        { Long value -> value.intValue( ) } as Function<Long, Integer> )
  }

  String toString( ) {
    lower == upper ?
        PrivateAddresses.fromInteger( lower ) :
        "${PrivateAddresses.fromInteger( lower )}-${PrivateAddresses.fromInteger( upper )}"
  }

  private static UnsignedInteger unsigned( int value ) {
    UnsignedInteger.fromIntBits( value )
  }

  private static class LongSequentialIterator extends AbstractSequentialIterator<Long> implements Iterator<Long> {
    private long last

    LongSequentialIterator( Integer first, Integer last ) {
      this( UnsignedInts.toLong( first ), UnsignedInts.toLong( last ) )
    }

    LongSequentialIterator( Long first, Long last ) {
      super( first )
      this.last = last
    }

    @Override
    protected Long computeNext( final Long value ) {
      value >= last ? null : value + 1
    }
  }

  private static enum IPRangeParse implements Function<String,Optional<IPRange>> {
    INSTANCE;

    @Override
    Optional<IPRange> apply( @Nullable final String range ) {
      try {
        Optional.of( parse( range ) )
      } catch ( IllegalArgumentException ) {
        Optional.absent( )
      }
    }
  }
}
