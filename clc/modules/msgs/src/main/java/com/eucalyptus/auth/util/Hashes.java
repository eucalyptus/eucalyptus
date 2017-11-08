/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

package com.eucalyptus.auth.util;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.UrlBase64;

/**
 * This class is headed for the dead pool.
 *
 * @deprecated
 */
@Deprecated
public class Hashes {
  public static Logger LOG = Logger.getLogger( Hashes.class );

  /**
   * DO NOT USE THIS
   *
   * @deprecated
   * @see com.google.common.io.BaseEncoding#base64Url( ).
   */
  @Deprecated
  public static String base64encode( String input ) {
    return new String( UrlBase64.encode( input.getBytes( ) ) );
  }

  /**
   * DO NOT USE THIS
   *
   * @deprecated
   * @see com.google.common.io.BaseEncoding#base64Url( ).
   */
  @Deprecated
  public static String base64decode( String input ) {
    return new String( UrlBase64.decode( input.getBytes( ) ) );
  }

  /**
   * DO NOT USE THIS
   *
   * @deprecated
   * @see com.google.common.io.BaseEncoding#base16( ).
   */
  @Deprecated
  public static String getHexString( byte[] data ) {
    StringBuffer buf = new StringBuffer( );
    for ( int i = 0; i < data.length; i++ ) {
      int halfbyte = ( data[i] >>> 4 ) & 0x0F;
      int two_halfs = 0;
      do {
        if ( ( 0 <= halfbyte ) && ( halfbyte <= 9 ) ) buf.append( ( char ) ( '0' + halfbyte ) );
        else buf.append( ( char ) ( 'a' + ( halfbyte - 10 ) ) );
        halfbyte = data[i] & 0x0F;
      } while ( two_halfs++ < 1 );
    }
    return buf.toString( ).toLowerCase( );
  }

  /**
   * DO NOT USE THIS
   *
   * @deprecated
   * @see com.google.common.io.BaseEncoding#base16( ).
   */
  @Deprecated
  public static byte[] hexToBytes( String data ) {
    int k = 0;
    byte[] results = new byte[data.length( ) / 2];
    for ( int i = 0; i < data.length( ); ) {
      results[k] = ( byte ) ( Character.digit( data.charAt( i++ ), 16 ) << 4 );
      results[k] += ( byte ) ( Character.digit( data.charAt( i++ ), 16 ) );
      k++;
    }

    return results;
  }

  /**
   * DO NOT USE THIS
   *
   * @deprecated
   * @see com.google.common.io.BaseEncoding#base16( ).
   */
  @Deprecated
  public static String bytesToHex( byte[] data ) {
    StringBuffer buffer = new StringBuffer( );
    for ( int i = 0; i < data.length; i++ ) {
      buffer.append( byteToHex( data[i] ) );
    }
    return ( buffer.toString( ) );
  }

  private static String byteToHex( byte data ) {
    StringBuffer hexString = new StringBuffer( );
    hexString.append( toHex( ( data >>> 4 ) & 0x0F ) );
    hexString.append( toHex( data & 0x0F ) );
    return hexString.toString( );
  }

  private static char toHex( int value ) {
    if ( ( 0 <= value ) && ( value <= 9 ) ) return ( char ) ( '0' + value );
    else return ( char ) ( 'a' + ( value - 10 ) );
  }

}
