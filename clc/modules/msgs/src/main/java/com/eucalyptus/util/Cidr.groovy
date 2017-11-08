/*************************************************************************
 * Copyright 2013-2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.util

import com.google.common.base.Optional
import com.google.common.base.Predicate
import com.google.common.base.Splitter
import com.google.common.collect.Lists
import com.google.common.net.InetAddresses
import com.google.common.primitives.Ints
import com.google.common.primitives.Longs
import com.google.common.primitives.UnsignedInteger
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
  static Cidr parse( String cidr ) {
    parse( cidr, false )
  }

  /**
   * Parse the given CIDR notation.
   *
   * @param cidr The CIDR string
   * @param lax True to normalize the CIDR on parse (allow mismatched ip/prefix)
   * @return The Cidr representation
   * @throws IllegalArgumentException If the CIDR text is invalid
   */
  static Cidr parse( final String cidr, final boolean lax ) {
    Parameters.checkParam( "cidr", cidr, notNullValue( ) )
    final Iterable<String> parts = Splitter.on('/').limit(2).split( cidr )
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
    if ( lax ) { // clear any excessive ip bits for the prefix
      ip = ip & prefixMask( prefix )
    }
    final BigInteger ipBigInteger = new BigInteger( Ints.toByteArray( ip ) )
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

  boolean contains( Cidr cidr ) {
    ( cidr.ip & prefixMask( prefix ) ) == this.ip && cidr.prefix >= this.prefix
  }

  Predicate<Cidr> contains( ) {
    { Cidr cidr -> contains( cidr ) } as Predicate<Cidr>
  }

  Predicate<Cidr> containedBy( ) {
    { Cidr cidr -> cidr.contains( this ) } as Predicate<Cidr>
  }

  Iterable<Cidr> split( final int parts ) {
    final List<Cidr> cidrs = Lists.newArrayList( )
    final int bits = parts==1 ? 0 : Numbers.log2( parts - 1 ) + 1
    final int maxParts = (int) Math.pow( 2, bits )
    final UnsignedInteger size = UnsignedInteger.fromIntBits( (int) Math.pow( 2, ( 32 - prefix ) ) ).dividedBy( UnsignedInteger.fromIntBits( maxParts ) )
    for ( int i=0; i < parts; i++ ) {
      cidrs.add( of( UnsignedInteger.fromIntBits( getIp( ) ).plus( size.times( UnsignedInteger.fromIntBits( i ) ) ).intValue( ), prefix + bits ) )
    }
    cidrs
  }

  static int prefixMask( int prefix ) {
    if ( prefix == 0 ) return 0
    BitSet bitSet = new BitSet( 32 )
    bitSet.flip( 32 - prefix, 32 )
    byte[] bytes = Longs.toByteArray( bitSet.toLongArray( )[0] )
    Ints.fromBytes( bytes[4], bytes[5], bytes[6], bytes[7] )
  }

  static NonNullFunction<String, Optional<Cidr>> parse( ) {
    CidrParse.INSTANCE
  }

  static NonNullFunction<Cidr, Integer> prefix( ) {
    CidrPrefix.INSTANCE
  }

  /**
   * @return A function that can throw IllegalArgumentException on parsing
   */
  static NonNullFunction<String, Cidr> parseUnsafe( ) {
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

  private static enum CidrParse implements NonNullFunction<String,Optional<Cidr>> {
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

  private static enum CidrParseUnsafe implements NonNullFunction<String,Cidr> {
    INSTANCE;

    @Override
    Cidr apply( @Nullable final String cidr ) {
      parse( cidr )
    }
  }

  private static enum CidrPrefix implements NonNullFunction<Cidr,Integer> {
    INSTANCE;

    @Override
    Integer apply( @Nullable final Cidr cidr ) {
      cidr?.prefix
    }
  }
}
