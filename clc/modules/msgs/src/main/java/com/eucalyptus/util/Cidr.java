package com.eucalyptus.util;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import org.hamcrest.Matchers;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedInteger;

public class Cidr implements Predicate<InetAddress> {

  private final int ip;
  private final int prefix;

  public Cidr( final int ip, final int prefix ) {
    this.ip = ip;
    this.prefix = prefix;
  }

  public static Cidr of( int address, int prefix ) {
    return new Cidr( address, prefix );
  }

  /**
   * Parse the given CIDR notation.
   *
   * @param cidr The CIDR string
   * @return The Cidr representation
   * @throws IllegalArgumentException If the CIDR text is invalid
   */
  public static Cidr parse( String cidr ) {
    return parse( cidr, false );
  }

  /**
   * Parse the given CIDR notation.
   *
   * @param cidr The CIDR string
   * @param lax  True to normalize the CIDR on parse (allow mismatched ip/prefix)
   * @return The Cidr representation
   * @throws IllegalArgumentException If the CIDR text is invalid
   */
  public static Cidr parse( final String cidr, final boolean lax ) {
    Parameters.checkParam( "cidr", cidr, Matchers.notNullValue( ) );
    final Iterable<String> parts = Splitter.on( "/" ).limit( 2 ).split( cidr );
    final String ipPart = Iterables.get( parts, 0, null );
    if ( ipPart == null ) {
      throw new IllegalArgumentException( "Invalid cidr: " + cidr );
    }

    if ( !InetAddresses.isInetAddress( ipPart ) ) {
      throw new IllegalArgumentException( "Invalid cidr: " + cidr );
    }

    int ip = InetAddresses.coerceToInteger( InetAddresses.forString( ipPart ) );
    final String prefixPart = Iterables.get( parts, 1, null );
    int prefix;
    try {
      prefix = ( (int) ( prefixPart == null ? 32 : Integer.parseInt( prefixPart ) ) );
    } catch ( NumberFormatException e ) {
      throw new IllegalArgumentException( "Invalid prefix in cidr: " + cidr );
    }

    if ( prefix < 0 || prefix > 32 ) {
      throw new IllegalArgumentException( "Invalid prefix in cidr: " + cidr );
    }

    if ( lax ) {// clear any excessive ip bits for the prefix
      ip = ip & prefixMask( prefix );
    }

    final BigInteger ipBigInteger = new BigInteger( Ints.toByteArray( ip ) );
    for ( int i = 0; i < 32 - prefix; i++ ) {
      if ( ipBigInteger.testBit( i ) ) {
        final Cidr suggestion = new Cidr( (int) ip & prefixMask( prefix ), prefix );
        throw new IllegalArgumentException( "Invalid address for prefix in cidr: " + cidr + " (try " + String.valueOf( suggestion ) + ")" );
      }

    }

    return new Cidr( ip, prefix );
  }

  /**
   * Create a CIDR from the given address / prefix length
   *
   * @param address The address to use
   * @param prefix  The network prefix length
   * @return The CIDR
   */
  public static Cidr fromAddress( final InetAddress address, final int prefix ) {
    BigInteger addressBigInteger = new BigInteger( address.getAddress( ) );
    for ( int i = 0; i < 32 - prefix; i++ ) {
      addressBigInteger = addressBigInteger.clearBit( i );
    }

    return new Cidr( addressBigInteger.intValue( ), prefix );
  }

  public static int prefixMask( int prefix ) {
    if ( prefix == 0 ) return 0;
    BitSet bitSet = new BitSet( 32 );
    bitSet.flip( (int) 32 - prefix, 32 );
    byte[] bytes = Longs.toByteArray( bitSet.toLongArray( )[ 0 ] );
    return Ints.fromBytes( bytes[ 4 ], bytes[ 5 ], bytes[ 6 ], bytes[ 7 ] );
  }

  public static NonNullFunction<String, Optional<Cidr>> parse( ) {
    return CidrParse.STRICT;
  }

  public static NonNullFunction<String, Optional<Cidr>> parseLax( ) {
    return CidrParse.LAX;
  }

  public static NonNullFunction<Cidr, Integer> prefix( ) {
    return CidrPrefix.INSTANCE;
  }

  /**
   * @return A function that can throw IllegalArgumentException on parsing
   */
  public static NonNullFunction<String, Cidr> parseUnsafe( ) {
    return ( (NonNullFunction<String, Cidr>) ( CidrParseUnsafe.INSTANCE ) );
  }

  public int getIp( ) {
    return ip;
  }

  public int getPrefix( ) {
    return prefix;
  }

  public String getIpAsText( ) {
    return InetAddresses.toAddrString( InetAddresses.fromInteger( ip ) );
  }

  public boolean contains( String ip ) {
    Optional<Cidr> cidr = parse( ).apply( ip );
    return cidr.isPresent( ) ? contains( cidr.get( ).ip ) : false;
  }

  public boolean contains( int ip ) {
    return ( ip & prefixMask( prefix ) ) == this.ip;
  }

  public boolean contains( Cidr cidr ) {
    return ( cidr.ip & prefixMask( prefix ) ) == this.ip && cidr.prefix >= this.prefix;
  }

  public Predicate<Cidr> contains( ) {
    return cidr -> contains( cidr );
  }

  public Predicate<Cidr> containedBy( ) {
    return cidr -> cidr.contains( this );
  }

  public Iterable<Cidr> split( final int parts ) {
    final List<Cidr> cidrs = Lists.newArrayList( );
    final int bits = parts == 1 ? 0 : Numbers.log2( (int) parts - 1 ) + 1;
    final int maxParts = (int) Math.pow( 2, bits );
    final UnsignedInteger size = UnsignedInteger.fromIntBits( (int) Math.pow( 2, (double) ( 32 - prefix ) ) ).dividedBy( UnsignedInteger.fromIntBits( maxParts ) );
    for ( int i = 0; i < parts; i++ ) {
      ( (ArrayList<Cidr>) cidrs ).add( of( UnsignedInteger.fromIntBits( getIp( ) ).plus( size.times( UnsignedInteger.fromIntBits( i ) ) ).intValue( ), (int) prefix + bits ) );
    }

    return cidrs;
  }

  public String toString( ) {
    return getIpAsText( ) + "/" + String.valueOf( prefix );
  }

  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !getClass( ).equals( o.getClass( ) ) ) return false;

    final Cidr cidr = (Cidr) o;

    if ( ip != cidr.ip ) return false;
    if ( prefix != cidr.prefix ) return false;

    return true;
  }

  public int hashCode( ) {
    int result;
    result = ip;
    result = 31 * result + prefix;
    return result;
  }

  /**
   * Predicate for matching contained InetAddresses
   *
   * @param inetAddress The address to check
   * @return true if this Cidr contains the given address
   */
  @Override
  public boolean apply( @Nullable final InetAddress inetAddress ) {
    return inetAddress == null ? false : contains( InetAddresses.coerceToInteger( inetAddress ) );
  }

  private enum CidrParse implements NonNullFunction<String, Optional<Cidr>> {
    STRICT( false ), LAX( true );

    private boolean lax;

    CidrParse( final boolean lax ) {
      this.lax = lax;
    }

    @Override
    public Optional<Cidr> apply( @Nullable final String cidr ) {
      try {
        return Optional.of( parse( cidr, this.lax ) );
      } catch ( Exception IllegalArgumentException ) {
        return Optional.absent( );
      }

    }
  }

  private enum CidrParseUnsafe implements NonNullFunction<String, Cidr> {
    INSTANCE;

    @Override
    public Cidr apply( @Nullable final String cidr ) {
      return parse( cidr );
    }

  }

  private enum CidrPrefix implements NonNullFunction<Cidr, Integer> {
    INSTANCE;

    @Override
    public Integer apply( @Nullable final Cidr cidr ) {
      return ( cidr == null ? null : cidr.getPrefix( ) );
    }

  }
}
