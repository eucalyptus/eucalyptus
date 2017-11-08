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

package com.eucalyptus.crypto.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.io.pem.PemObject;

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
  
  @Nullable
  public static KeyPair toKeyPair( String keyPairB64PemString ) {
    return PEMFiles.getKeyPair( B64.url.dec( keyPairB64PemString ) );
  }
  
  public static void write( final String fileName, final Object securityToken ) {
    PEMWriter privOut;
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

  public static byte[] getBytes( final String type, final byte[] bytes ) {
    return getBytes( new PemObject( type, bytes ) );
  }
  
  public static X509Certificate getCert( final byte[] o ) {
    X509Certificate x509 = null;
    ByteArrayInputStream pemByteIn = new ByteArrayInputStream( o );
    try ( PEMParser in = new PEMParser( new InputStreamReader( pemByteIn ) ) ) {
      x509 = new JcaX509CertificateConverter( ).setProvider( BouncyCastleProvider.PROVIDER_NAME )
          .getCertificate( (X509CertificateHolder) in.readObject() );
    } catch ( IOException | CertificateException e ) {
      LOG.error( e, e );//this can never happen
    }
    return x509;
  }

  @Nullable
  public static KeyPair getKeyPair( final byte[] o ) {
    KeyPair keyPair = null;
    ByteArrayInputStream pemByteIn = new ByteArrayInputStream( o );
    try ( PEMParser in = new PEMParser( new InputStreamReader( pemByteIn ) )  )  {
      final Object keyObj = in.readObject();
      if (keyObj instanceof PEMKeyPair) {
        final PEMKeyPair pemKeyPair = (PEMKeyPair) keyObj;
        if ( pemKeyPair != null ) {
          keyPair = 
              new JcaPEMKeyConverter( ).setProvider( BouncyCastleProvider.PROVIDER_NAME ).getKeyPair( pemKeyPair );
        }
      }else if (keyObj instanceof PrivateKeyInfo) {
        final PrivateKeyInfo pKeyInfo = (PrivateKeyInfo) keyObj;
        final PrivateKey pKey = new JcaPEMKeyConverter( ).setProvider( BouncyCastleProvider.PROVIDER_NAME ).getPrivateKey(pKeyInfo);
        if( pKey != null ) {
          keyPair = new KeyPair(null, pKey);
        }
      }
    } catch ( IOException e ) {
      LOG.error( e, e );//this can never happen
    }
    return keyPair;
  }
}
