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

package com.eucalyptus.auth.crypto;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.util.encoders.UrlBase64;

public class StringCryptoTest {
  
  /**
   * @param args
   */
  public static void main( String[] args ) throws Exception {
    // TODO Auto-generated method stub
    StringCrypto sc = new StringCrypto( "RSA/ECB/PKCS1Padding", "BC" );
    System.out.println( sc.decryptOpenssl( "N8MFkU9cbxKHvtD9Xq0JaAu2X65d90J1lD6wJ5UkcdX4LyUZv/sBtaa0HlXZlW64YoAzn02P+312GTTsGiUlBzbK8o5LbY8DHyOqH/thv3JhvLVLpQRTLBH+YnGzBwqybUnwGTz4dNxkKu52vA/FvGC7UNC/PHzxjN07CwZ1riJPoYB6vSyH41dVYbs+oLSm2FMXx+mLxKVYq4NoewSPiwn0fZHTITm6nvWi5IV2cNF+K+Ibgx9/QUanKHRjAmmvEHVIGQoXu72POkTjdNu+tqqNFN7jF3dD0/CuXVeSYx/auOHhQ6zTnDJdqPHWd2H9CQQU+nfHtsR3VG91vE73yA==" ) );
  }
  
  private static void printDec( byte[] ba ) {
    for ( byte b : ba ) {
      System.out.print( b + " " );
    }
    System.out.print( '\n' );
  }
  
  private static byte[] readfile( String filename ) throws Exception {
    FileInputStream fis = new FileInputStream( filename );
    ByteArrayOutputStream baos = new ByteArrayOutputStream( );
    byte[] block = new byte[512];
    int n;
    while ( ( n = fis.read( block ) ) > 0 ) {
      baos.write( block, 0, n );
    }
    byte[] bytes = baos.toByteArray( );
    baos.close( );
    return bytes;
  }
  
}
