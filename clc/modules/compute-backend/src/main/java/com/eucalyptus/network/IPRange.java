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

import static com.eucalyptus.util.FUtils.vOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.hamcrest.Matchers;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.AbstractSequentialIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedInts;
import io.vavr.control.Option;

/**
 *
 */
public class IPRange implements Iterable<Integer> {

  IPRange( final int lower, final int upper ) {
    this.lower = lower;
    this.upper = upper;
  }

  /**
   * Parse an ip range, which may be a single value or a range.
   *
   * @throws IllegalArgumentException if the given range is invalid
   */
  public static IPRange parse( final String range ) {
    Parameters.checkParam( "range", range, Matchers.notNullValue( ) );
    final Iterable<String> parts = Splitter.on( "-" ).trimResults( ).omitEmptyStrings( ).limit( 2 ).split( range );
    final String lowerPart = Iterables.get( parts, 0, null );
    if ( lowerPart == null ) {
      throw new IllegalArgumentException( "Invalid range: " + range );
    }
    final String upperPart = Iterables.get( parts, 1, lowerPart );
    //noinspection ConstantConditions
    if ( !InetAddresses.isInetAddress( lowerPart ) || !InetAddresses.isInetAddress( upperPart ) ) {
      throw new IllegalArgumentException( "Invalid range: " + range );
    }

    final int lower = PrivateAddresses.asInteger( lowerPart );
    final int upper = PrivateAddresses.asInteger( upperPart );
    if ( UnsignedInts.compare( lower, upper ) > 0 ) {
      throw new IllegalArgumentException( "Invalid range: " + range );
    }

    return new IPRange( lower, upper );
  }

  public static CompatFunction<String, Optional<IPRange>> parse( ) {
    return IPRangeParse.INSTANCE;
  }

  public static CompatFunction<String, Option<IPRange>> optParse( ) {
    return vOption( IPRangeParse.INSTANCE );
  }

  /**
   * Create an ip range for the given subnet and netmask
   *
   * @throws IllegalArgumentException if the given subnet or netmask is invalid
   */
  public static IPRange fromSubnet( final String subnet, final String netmask ) {
    Parameters.checkParam( "subnet", subnet, Matchers.notNullValue( ) );
    Parameters.checkParam( "netmask", netmask, Matchers.notNullValue( ) );
    if ( !InetAddresses.isInetAddress( subnet ) || !InetAddresses.isInetAddress( netmask ) ) {
      throw new IllegalArgumentException( "Invalid subnet/netmask: " + subnet + "/" + netmask );
    }

    final int snet = PrivateAddresses.asInteger( subnet );
    final int mask = PrivateAddresses.asInteger( netmask );
    return new IPRange( snet & mask, snet | ( ~mask ) ).perhapsShrink( );
  }

  /**
   * Create an ip range for the given cidr
   */
  public static IPRange fromCidr( Cidr cidr ) {
    Parameters.checkParam( "cidr", cidr, Matchers.notNullValue( ) );
    final int snet = cidr.getIp( );
    final int mask = Cidr.prefixMask( cidr.getPrefix( ) );
    return new IPRange( snet & mask, snet | ( ~mask ) ).perhapsShrink( );
  }

  @SuppressWarnings( "ConstantConditions" )
  public static boolean isIPRange( String range ) {
    return parse( ).apply( range ).isPresent( );
  }

  /**
   * Split this range around ip, omitting ip from the resulting ranges.
   */
  @SuppressWarnings( "ArraysAsListWithZeroOrOneArgument" )
  public List<IPRange> split( final String ip ) {
    Parameters.checkParam( "ip", ip, Matchers.notNullValue( ) );
    if ( !InetAddresses.isInetAddress( ip ) ) {
      throw new IllegalArgumentException( "Invalid ip: " + ip );
    }

    final int address = PrivateAddresses.asInteger( ip );
    if ( UnsignedInts.compare( address, lower ) > 0 && UnsignedInts.compare( address, upper ) < 0 ) {
      return Arrays.asList( new IPRange( lower, unsigned( address ).minus( unsigned( 1 ) ).intValue( ) ), new IPRange( unsigned( address ).plus( unsigned( 1 ) ).intValue( ), upper ) );
    } else if ( UnsignedInts.compare( lower, address ) == 0 ) {
      return Arrays.asList( new IPRange( unsigned( address ).plus( unsigned( 1 ) ).intValue( ), upper ) );
    } else if ( UnsignedInts.compare( upper, address ) == 0 ) {
      return Arrays.asList( new IPRange( lower, unsigned( address ).minus( unsigned( 1 ) ).intValue( ) ) );
    } else {
      return Arrays.asList( this );
    }
  }

  /**
   * Does this range (inclusively) contain the given range.
   *
   * @param range The range to test
   */
  public boolean contains( IPRange range ) {
    return UnsignedInts.compare( lower, range.lower ) < 1 && UnsignedInts.compare( upper, range.upper ) > -1;
  }

  /**
   * Shrink this range if possible by one at each end.
   *
   * <p>Useful to skip the network address and broadcast address in a subnet.</p>
   */
  @SuppressWarnings( "WeakerAccess" )
  public IPRange perhapsShrink( ) {
    UnsignedInteger shrinkLower = unsigned( lower ).plus( unsigned( 1 ) );
    UnsignedInteger shrinkUpper = unsigned( upper ).minus( unsigned( 1 ) );
    return shrinkLower.compareTo( shrinkUpper ) <= 0 ? new IPRange( shrinkLower.intValue( ), shrinkUpper.intValue( ) ) : this;
  }

  @Nonnull
  @Override
  public Iterator<Integer> iterator( ) {
    return Iterators.transform( new LongSequentialIterator( lower, upper ), Long::intValue );
  }

  public int size( ) {
    return 1 + ( unsigned( upper ).minus( unsigned( lower ) ).intValue( ) );
  }

  public String toString( ) {
    return lower == upper ? PrivateAddresses.fromInteger( lower ) : PrivateAddresses.fromInteger( lower ) + "-" + PrivateAddresses.fromInteger( upper );
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final IPRange integers = (IPRange) o;
    return lower == integers.lower &&
        upper == integers.upper;
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( lower, upper );
  }

  private static UnsignedInteger unsigned( int value ) {
    return UnsignedInteger.fromIntBits( value );
  }

  private final int lower;
  private final int upper;

  private static class LongSequentialIterator extends AbstractSequentialIterator<Long> implements Iterator<Long> {

    LongSequentialIterator( Integer first, Integer last ) {
      this( UnsignedInts.toLong( first ), UnsignedInts.toLong( last ) );
    }

    LongSequentialIterator( Long first, Long last ) {
      super( first );
      this.last = last;
    }

    @SuppressWarnings( "NullableProblems" )
    @Override
    protected Long computeNext( final Long value ) {
      return value >= last ? null : value + 1;
    }

    private long last;
  }

  private enum IPRangeParse implements CompatFunction<String,Optional<IPRange>> {
    INSTANCE;

    @SuppressWarnings( "Guava" )
    @Override
    public Optional<IPRange> apply( @Nullable final String range ) {
      try {
        return Optional.of( parse( range ) );
      } catch ( Exception IllegalArgumentException ) {
        return Optional.absent( );
      }

    }

  }
}
