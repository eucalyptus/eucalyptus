/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.auth.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.encoders.UrlBase64;

/**
 * This class is headed for the dead pool.
 */
@Deprecated
public class Hashes {
  public static Logger LOG = Logger.getLogger( Hashes.class );


  /**
   * TODO: Move this up in the dependency tree.
   * @param o
   * @return
   */
  @Deprecated
  public static X509Certificate getPemCert( final byte[] o ) {
    X509Certificate x509 = null;
    PEMReader in = null;
    ByteArrayInputStream pemByteIn = new ByteArrayInputStream( o );
    in = new PEMReader( new InputStreamReader( pemByteIn ) );
    try {
      x509 = ( X509Certificate ) in.readObject( );
    } catch ( IOException e ) {
      LOG.error( e, e );//this can never happen
    }
    return x509;
  }

  /**
   * TODO: Move this up in the dependency tree.
   * @param o
   * @return
   */
  @Deprecated
  public static String base64encode( String input ) {
    return new String( UrlBase64.encode( input.getBytes( ) ) );
  }

  /**
   * TODO: Move this up in the dependency tree.
   * @param o
   * @return
   */
  @Deprecated
  public static String base64decode( String input ) {
    return new String( UrlBase64.decode( input.getBytes( ) ) );
  }

  /**
   * TODO: Move this up in the dependency tree.
   * @param o
   * @return
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

  public static String getRandom( int size ) {
    SecureRandom random = new SecureRandom( );
    byte[] randomBytes = new byte[size];
    random.nextBytes( randomBytes );
    return new String( UrlBase64.encode( randomBytes ) );
  }

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

  public static String bytesToHex( byte[] data ) {
    StringBuffer buffer = new StringBuffer( );
    for ( int i = 0; i < data.length; i++ ) {
      buffer.append( byteToHex( data[i] ) );
    }
    return ( buffer.toString( ) );
  }

  public static String byteToHex( byte data ) {
    StringBuffer hexString = new StringBuffer( );
    hexString.append( toHex( ( data >>> 4 ) & 0x0F ) );
    hexString.append( toHex( data & 0x0F ) );
    return hexString.toString( );
  }

  public static char toHex( int value ) {
    if ( ( 0 <= value ) && ( value <= 9 ) ) return ( char ) ( '0' + value );
    else return ( char ) ( 'a' + ( value - 10 ) );
  }

}
