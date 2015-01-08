/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.util

import com.google.common.base.Function
import com.google.common.base.Optional
import com.google.common.base.Predicate
import com.google.common.base.Splitter
import com.google.common.net.InetAddresses
import com.google.common.primitives.Ints
import com.google.common.primitives.Longs
import groovy.transform.CompileStatic
import groovy.transform.Immutable

import javax.annotation.Nullable

import static org.hamcrest.Matchers.notNullValue

@CompileStatic
@Immutable
class Cidr implements Predicate<InetAddress> {
  int ip;
  int prefix;

  static Cidr of( int address, int prefix ) {
    new Cidr( address, prefix )
  }

  /**
   * Parse the given CIDR notation.
   *
   * @param cidr The CIDR string
   * @return The Cidr representation
   * @throws IllegalArgumentException If the CIDR text is invalid
   */
  static Cidr parse( final String cidr ) {
    Parameters.checkParam( "cidr", cidr, notNullValue( ) )
    final Iterable<String> parts = Splitter.on('/').trimResults().omitEmptyStrings().limit(2).split( cidr )
    final String ipPart = parts.getAt( 0 )
    if ( ipPart == null ) {
      throw new IllegalArgumentException( "Invalid cidr: ${cidr}" )
    }
    if ( !InetAddresses.isInetAddress( ipPart ) ) {
      throw new IllegalArgumentException( "Invalid cidr: ${cidr}" )
    }
    int ip = InetAddresses.coerceToInteger( InetAddresses.forString( ipPart ) )
    final String prefixPart = parts.getAt( 1 )
    int prefix
    try {
      prefix = prefixPart == null ?
          32 :
          Integer.parseInt( prefixPart )
    } catch ( NumberFormatException e ) {
      throw new IllegalArgumentException( "Invalid prefix in cidr: ${cidr}" )
    }
    if ( prefix < 0 || prefix > 32 ) {
      throw new IllegalArgumentException( "Invalid prefix in cidr: ${cidr}" )
    }
    final BigInteger ipBigInteger = new BigInteger( InetAddresses.forString( ipPart ).address )
    for ( int i=0; i < 32 - prefix; i++ ) {
      if ( ipBigInteger.testBit( i ) ) {
        Cidr suggestion = new Cidr( ip & prefixMask( prefix ), prefix )
        throw new IllegalArgumentException( "Invalid address for prefix in cidr: ${cidr} (try ${suggestion})" )
      }
    }
    new Cidr( ip, prefix )
  }

  /**
   * Create a CIDR from the given address / prefix length
   *
   * @param address The address to use
   * @param prefix The network prefix length
   * @return The CIDR
   */
  static Cidr fromAddress( final InetAddress address, final int prefix ) {
    BigInteger addressBigInteger = new BigInteger( address.address )
    for ( int i=0; i < 32 - prefix; i++ ) {
      addressBigInteger = addressBigInteger.clearBit( i )
    }
    new Cidr( addressBigInteger.intValue( ), prefix )
  }

  String getIpAsText() {
    InetAddresses.toAddrString( InetAddresses.fromInteger( ip ) )
  }

  boolean contains( String ip ) {
    Optional<Cidr> cidr = parse( ).apply( ip )
    cidr.present ?
        contains( cidr.get( ).ip ) :
        false
  }

  boolean contains( int ip ) {
    ( ip & prefixMask( prefix ) ) == this.ip
  }

  private static int prefixMask( int prefix ) {
    if ( prefix == 0 ) return 0
    BitSet bitSet = new BitSet( 32 )
    bitSet.flip( 32 - prefix, 32 )
    byte[] bytes = Longs.toByteArray( bitSet.toLongArray( )[0] )
    Ints.fromBytes( bytes[4], bytes[5], bytes[6], bytes[7] )
  }

  static Function<String, Optional<Cidr>> parse( ) {
    CidrParse.INSTANCE
  }

  /**
   * @return A function that can throw IllegalArgumentException on parsing
   */
  static Function<String, Cidr> parseUnsafe( ) {
    CidrParseUnsafe.INSTANCE
  }

  String toString( ) {
    "${ipAsText}/${prefix}"
  }

  boolean equals(final Object o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    final Cidr cidr = (Cidr) o

    if (ip != cidr.ip) return false
    if (prefix != cidr.prefix) return false

    return true
  }

  int hashCode() {
    int result
    result = ip
    result = 31 * result + prefix
    return result
  }

  /**
   * Predicate for matching contained InetAddresses
   *
   * @param inetAddress The address to check
   * @return true if this Cidr contains the given address
   */
  @Override
  boolean apply( @Nullable final InetAddress inetAddress ) {
    inetAddress == null ?
        false :
        contains( InetAddresses.coerceToInteger( inetAddress ) )
  }

  private static enum CidrParse implements Function<String,Optional<Cidr>> {
    INSTANCE;

    @Override
    Optional<Cidr> apply( @Nullable final String cidr ) {
      try {
        Optional.of( parse( cidr ) )
      } catch ( IllegalArgumentException ) {
        Optional.absent( )
      }
    }
  }

  private static enum CidrParseUnsafe implements Function<String,Cidr> {
    INSTANCE;

    @Override
    Cidr apply( @Nullable final String cidr ) {
      parse( cidr )
    }
  }
}
