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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.crypto;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Calendar;
import java.util.zip.Adler32;
import javax.security.auth.x500.X500Principal;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.util.encoders.UrlBase64;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.CertificateProvider;
import com.eucalyptus.crypto.CryptoProvider;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.crypto.HmacProvider;
import com.eucalyptus.crypto.Signatures;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.google.common.primitives.Longs;

public final class DefaultCryptoProvider implements CryptoProvider, CertificateProvider, HmacProvider {
  public static String        KEY_ALGORITHM         = "RSA";
  private static final String KEY_SIGNING_ALGORITHM = "SHA512WithRSA";
  private static final int    KEY_SIZE              = 2048;
  public static String        PROVIDER              = "BC";
  private static final String PRIVATE_KEY_FORMAT    = System.getProperty( DefaultCryptoProvider.class.getName() + ".privateKeyFormat", "" );
  private static Logger       LOG                   = Logger.getLogger( DefaultCryptoProvider.class );
  
  public DefaultCryptoProvider( ) {}
  
  /**
   * @see com.eucalyptus.crypto.CryptoProvider#generateHashedPassword(java.lang.String)
   */
  @Override
  public String generateHashedPassword( String password ) {
    byte[] data = Digest.MD5.get( ).digest( password.getBytes( ) );
    StringBuffer buf = new StringBuffer( );
    for ( int i = 0; i < data.length; i++ ) {
      int halfbyte = ( data[i] >>> 4 ) & 0x0F;
      int two_halfs = 0;
      do {
        if ( ( 0 <= halfbyte ) && ( halfbyte <= 9 ) )
          buf.append( ( char ) ( '0' + halfbyte ) );
        else buf.append( ( char ) ( 'a' + ( halfbyte - 10 ) ) );
        halfbyte = data[i] & 0x0F;
      } while ( two_halfs++ < 1 );
    }
    return buf.toString( ).toLowerCase( );
  }
  
  /**
   * @see com.eucalyptus.crypto.CryptoProvider#generateAlphanumericId(int,String)
   */
  @Override
  public String generateAlphanumericId( final int length, 
                                        final String prefix ) {
    return prefix + generateRandomAlphanumeric( length - prefix.length() ).toUpperCase();//NOTE: this MUST be upper case.
  }

  /**
   * @see com.eucalyptus.crypto.CryptoProvider#generateSecretKey()
   */
  @Override
  public String generateSecretKey() {
    return generateRandomAlphanumeric(40);//NOTE: this MUST be 40-chars from base64.
  }
  
  /**
   * Note that the output has not always been of fixed length.
   *
   * @see com.eucalyptus.crypto.CryptoProvider#generateSessionToken()
   */
  @Override
  public String generateSessionToken() {
    return generateRandomAlphanumeric(80);
  }

  private String generateRandomAlphanumeric(int length) {
    final StringBuilder randomBuilder = new StringBuilder( length + 90 );
    while( randomBuilder.length() < length ) {
        randomBuilder.append(generateRandomAlphanumeric() );
    }
    return randomBuilder.toString().substring( 0, length );
  }
    
  private String generateRandomAlphanumeric() {
    // length from generateRandomAlphanumeric is not constant due to
    // removal of punctuation characters
    return Hashes.getRandom( 64 ).replaceAll("\\p{Punct}", "");
  }

  /**
   * Note that the output is not standard Base64.
   *
   * <p>The output has the following substitutions:</p>
   *
   * <ul>
   *   <li> + -> - </li>
   *   <li> / -> _ </li>
   *   <li> = -> . </li>
   * </ul>
   *
   * @see com.eucalyptus.crypto.CryptoProvider#getDigestBase64(java.lang.String, com.eucalyptus.crypto.Digest)
   */
  @Override
  public String getDigestBase64( final String input, final Digest hash ) {
    final byte[] inputBytes = input.getBytes();
    final MessageDigest digest = hash.get( );
    digest.update( inputBytes );
    final byte[] digestBytes = digest.digest( );
    return new String( UrlBase64.encode( digestBytes ) );
  }

  public X509Certificate generateServiceCertificate( KeyPair keys, String serviceName ) {
    X500Principal x500 = new X500Principal( String.format( "CN=%s, OU=Eucalyptus, O=Cloud, C=US", serviceName ) );
//    if( !"eucalyptus".equals( serviceName ) ) {
//      SystemCredentials sys = SystemCredentials.lookup( Eucalyptus.class );
//      return generateCertificate( keys, x500, sys.getCertificate( ).getSubjectX500Principal( ), sys.getPrivateKey( ) );
//    } else {
      return generateCertificate( keys, x500, x500, null );
//    }
  }
  
  public X509Certificate generateCertificate( KeyPair keys, String userName ) {
    return generateCertificate( keys, new X500Principal( String.format( "CN=%s, OU=Eucalyptus, O=User, C=US", userName ) ) );
  }
  
  public X509Certificate generateCertificate( KeyPair keys, X500Principal dn ) {
    return generateCertificate( keys, dn, dn, null );
  }
  
  @Override
  public X509Certificate generateCertificate( KeyPair keys, X500Principal subjectDn, X500Principal signer, PrivateKey signingKey ) {
    signer = ( signingKey == null ? signer : subjectDn );
    signingKey = ( signingKey == null ? keys.getPrivate( ) : signingKey );
    EventRecord.caller( DefaultCryptoProvider.class, EventType.GENERATE_CERTIFICATE, signer.toString( ), subjectDn.toString( ) ).info();
    X509V3CertificateGenerator certGen = new X509V3CertificateGenerator( );
    certGen.setSerialNumber( BigInteger.valueOf( System.nanoTime( ) ).shiftLeft( 4 ).add( BigInteger.valueOf( ( long ) Math.rint( Math.random( ) * 1000 ) ) ) );
    certGen.setIssuerDN( signer );
    certGen.addExtension( X509Extensions.BasicConstraints, true, new BasicConstraints( true ) );
    try {
      certGen.addExtension( X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure( keys.getPublic( ) ) );
    } catch ( InvalidKeyException e ) {
      LOG.error( "Error adding subject key identifier extension.", e );
    }
    Calendar cal = Calendar.getInstance( );
    certGen.setNotBefore( cal.getTime( ) );
    cal.add( Calendar.YEAR, 5 );
    certGen.setNotAfter( cal.getTime( ) );
    certGen.setSubjectDN( subjectDn );
    certGen.setPublicKey( keys.getPublic( ) );
    certGen.setSignatureAlgorithm( KEY_SIGNING_ALGORITHM );
    try {
      X509Certificate cert = certGen.generate( signingKey, PROVIDER );
      cert.checkValidity( );
      return cert;
    } catch ( Exception e ) {
      LOG.fatal( e, e );
      return null;
    }
  }
  
  /**
   * @see com.eucalyptus.crypto.CertificateProvider#generateKeyPair()
   */
  @Override
  public KeyPair generateKeyPair( ) {
    KeyPairGenerator keyGen = null;
    try {
      EventRecord.caller( DefaultCryptoProvider.class, EventType.GENERATE_KEYPAIR );
      keyGen = KeyPairGenerator.getInstance( KEY_ALGORITHM, PROVIDER );
      SecureRandom random = new SecureRandom( );
    //TODO: RELEASE: see line:110
      keyGen.initialize( KEY_SIZE, random );
      KeyPair keyPair = keyGen.generateKeyPair( );
      return keyPair;
    } catch ( Exception e ) {
      LOG.fatal( e, e );
      return null;
    }
  }

  /**
   * Get the PKCS#8 encoded bytes for the key.
   *
   * @param privateKey The key to encode.
   * @return The bytes
   */
  @Override
  public byte[] getEncoded( final PrivateKey privateKey ) {
    if ( "pkcs8".equals( PRIVATE_KEY_FORMAT ) ) try {
      return KeyFactory.getInstance( KEY_ALGORITHM, PROVIDER )
          .getKeySpec( privateKey, PKCS8EncodedKeySpec.class ).getEncoded();
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    return privateKey.getEncoded();
  }

  @Override
  public String generateSystemSignature( ) {
    return this.generateSystemToken( ComponentIds.lookup( Eucalyptus.class ).name( ).getBytes( ) );
  }

  @Override
  public String generateSystemToken( byte[] data ) {
    PrivateKey pk = SystemCredentials.lookup( Eucalyptus.class ).getPrivateKey( );
    return Signatures.SHA256withRSA.trySign( pk, data );    
  }

  @Override
  public String generateId( final String seed, final String prefix ) {
    Adler32 hash = new Adler32( );
    String key = seed;
    hash.update( key.getBytes( ) );
    /**
     * @see http://tools.ietf.org/html/rfc3309
     */
    for ( int i = key.length( ); i < 128; i += 8 ) {
      hash.update( Longs.toByteArray( Double.doubleToRawLongBits( Math.random( ) ) ) );
    }
    String id = String.format( "%s-%08X", prefix, hash.getValue( ) );
    return id;
  }

  @Override
  public String getFingerPrint( Key privKey ) {
    return getFingerPrint( privKey.getEncoded( ) );
  }

  @Override
  public String getFingerPrint( byte[] data ) {
    try {
      byte[] fp = Digest.SHA1.get( ).digest( data );
      StringBuffer sb = new StringBuffer( );
      for ( byte b : fp )
        sb.append( String.format( "%02X:", b ) );
      return sb.substring( 0, sb.length( ) - 1 ).toLowerCase( );
    } catch ( Exception e ) {
      LOG.error( e, e );
      return null;
    }
  }

  @Override
  public String generateLinuxSaltedPassword(String password) {
    // Use MD5Crypt
    // TODO(wenye): try SHA256?
    return MD5Crypt.crypt( password );
  }

  @Override
  public boolean verifyLinuxSaltedPassword(String clear, String hashed) {
    // Use MD5Crypt
    // TODO(wenye): try SHA256?
    return MD5Crypt.verifyPassword( clear, hashed );
  }

}
