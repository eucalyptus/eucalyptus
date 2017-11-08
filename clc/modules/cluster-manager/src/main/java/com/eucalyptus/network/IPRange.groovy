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
package com.eucalyptus.network

import com.eucalyptus.util.Cidr
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

  /**
   * Create an ip range for the given cidr
   */
  static IPRange fromCidr( Cidr cidr ) {
    Parameters.checkParam( "cidr", cidr, notNullValue( ) )
    final int snet = cidr.ip
    final int mask = cidr.prefixMask( cidr.prefix )
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

  long size( ) {
    lower == upper ?
        1 :
        unsigned( upper ).minus( unsigned( lower ) ).longValue( )
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
