/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.policy.key;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

/**
 * RFC4632 IPv4 CIDR
 * 
 * http://tools.ietf.org/html/rfc4632
 */
public class Cidr {
  
  private static final Logger LOG = Logger.getLogger( Cidr.class );
  
  private static final Pattern CIDR_PATTERN = Pattern.compile( "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?:/(\\d{1,2}))?" );
  
  private static final int PREFIX_32 = 0xFFFFFFFF;
  
  private int prefix;
  private int mask;
  
  public Cidr( int prefix, int mask ) {
    this.prefix = prefix;
    this.mask = mask;
  }
  
  public int getPrefix( ) {
    return this.prefix;
  }
  
  public int getMask( ) {
    return this.mask;
  }
  
  public boolean matchIp( String ip ) {
    try {
      Cidr ipCidr = valueOf( ip );
      if ( ipCidr.getMask( ) != PREFIX_32 ) {
        throw new CidrParseException( );
      }
      return ( ipCidr.getPrefix( ) & this.mask ) == this.prefix;
    } catch ( CidrParseException e ) {
      // Input ip address is invalid.
      LOG.error( "Input IP address to match is invalid: " + ip, e );
    }
    return false;
  }
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    int bits = this.prefix;
    final int lsb8 = 0xFF;
    sb.append( ( bits >> 24 ) & lsb8 ).append( '.' );
    sb.append( ( bits >> 16 ) & lsb8 ).append( '.' );
    sb.append( ( bits >> 8 ) & lsb8 ).append( '.' );
    sb.append( bits & lsb8 );
    if ( this.mask != PREFIX_32 ) {
      int count = 0;
      int maskBits = this.mask;
      while ( maskBits != 0 ) {
        count++;
        maskBits <<= 1;
      }
      sb.append( '/' ).append( count );
    }
    return sb.toString( );
  }
  
  public static Cidr valueOf( String cidr ) throws CidrParseException {
    if ( cidr == null || "".equals( cidr ) ) {
      throw new CidrParseException( "Empty CIDR" );
    }
    Matcher matcher = CIDR_PATTERN.matcher( cidr );
    if ( !matcher.matches( ) ) {
      throw new CidrParseException( "Syntax error in input CIDR: " + cidr );
    }
    try {
      int prefix = 0;
      for ( int i = 1; i < 5; i++ ) {
        int component = Integer.parseInt( matcher.group(i) );
        if ( component > 255 ) {
          throw new CidrParseException( "Component value larger than 255: " + cidr );
        }
        prefix = ( prefix << 8 ) + component;
      }
      int mask = 0;
      if ( matcher.group( 5 ) != null ) {
        mask = Integer.parseInt( matcher.group( 5 ) );
        if ( mask > 32 ) {
          throw new CidrParseException( "Prefix length larger than 32: " + cidr );
        }
        if ( mask == 0 ) {
          mask = PREFIX_32;
        } else {
          mask = ( 1 << ( 32 - mask ) ) - 1;
        }
      }
      if ( ( prefix & mask ) != 0 ) {
        throw new CidrParseException( "Address and prefix length do not match: " + cidr );
      }
      return new Cidr( prefix, ~mask );
    } catch ( NumberFormatException e ) {
      // Impossible
      throw new CidrParseException( "Impossible number format error: " + cidr );
    }
  }
  
  // Tests
  public static void main( String[] args ) throws Exception {
    String ip = "192.168.0.0/16";
    System.out.println( ip + "->" + Cidr.valueOf( ip ).toString( ) );
    ip = "192.168.1.0/32";
    System.out.println( ip + "->" + Cidr.valueOf( ip ).toString( ) );
    ip = "0.0.0.0/0";
    System.out.println( ip + "->" + Cidr.valueOf( ip ).toString( ) );
    System.out.println( Cidr.valueOf( "192.168.0.0/16" ).matchIp( "192.168.0.1" ) ); // true
    System.out.println( Cidr.valueOf( "192.168.0.0/16" ).matchIp( "192.169.0.1" ) ); // false
    System.out.println( Cidr.valueOf( "0.0.0.0/0" ).matchIp( "192.168.0.1" ) ); // true
    System.out.println( Cidr.valueOf( "192.168.0.1/32" ).matchIp( "192.168.0.1" ) ); // true
  }
  
}
