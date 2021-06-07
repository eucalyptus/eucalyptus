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

package com.eucalyptus.crypto;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import org.junit.Ignore;

@Ignore("Manual development test")
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
