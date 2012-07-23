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

package com.eucalyptus.crypto.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.EventRecord;

public class PEMFiles {
  private static Logger LOG = Logger.getLogger( PEMFiles.class );

  public static String fromCertificate( X509Certificate x509 ) {
    return B64.url.encString( PEMFiles.getBytes( x509 ) );
  }
  
  public static X509Certificate toCertificate( String x509PemString ) {
    return PEMFiles.getCert( B64.url.dec( x509PemString ) );
  }

  public static String fromKeyPair( KeyPair keyPair ) {
    return B64.url.encString( PEMFiles.getBytes( keyPair ) );
  }
  
  public static KeyPair toKeyPair( String keyPairB64PemString ) {
    return PEMFiles.getKeyPair( B64.url.dec( keyPairB64PemString ) );
  }
  
  public static void write( final String fileName, final Object securityToken ) {
    PEMWriter privOut = null;
    try {
      privOut = new PEMWriter( new FileWriter( fileName ) );
      EventRecord.caller( PEMFiles.class, EventType.CERTIFICATE_WRITE, fileName ).info( );
      privOut.writeObject( securityToken );
      privOut.close( );
    } catch ( final IOException e ) {
      LOG.error( e, e );
    }
  }

  public static byte[] getBytes( final Object o ) {
    PEMWriter pemOut;
    ByteArrayOutputStream pemByteOut = new ByteArrayOutputStream( );
    try {
      pemOut = new PEMWriter( new OutputStreamWriter( pemByteOut ) );
      pemOut.writeObject( o );
      pemOut.close( );
    } catch ( IOException e ) {
      LOG.error( e, e );//this can never happen
    }
    return pemByteOut.toByteArray( );
  }

  public static X509Certificate getCert( final byte[] o ) {
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

  public static KeyPair getKeyPair( final byte[] o ) {
    KeyPair keyPair = null;
    PEMReader in = null;
    ByteArrayInputStream pemByteIn = new ByteArrayInputStream( o );
    in = new PEMReader( new InputStreamReader( pemByteIn ) );
    try {
      keyPair = ( KeyPair ) in.readObject( );
    } catch ( IOException e ) {
      LOG.error( e, e );//this can never happen
    }
    return keyPair;
  }
}
