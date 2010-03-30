/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.auth;

import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMWriter;
import com.eucalyptus.auth.crypto.CryptoProvider;
import com.eucalyptus.auth.crypto.Digest;
import com.eucalyptus.entities.EntityWrapper;

public class Credentials {
  static String         DB_NAME = "eucalyptus_auth";
  private static Logger         LOG     = Logger.getLogger( Credentials.class );
  private static CryptoProvider provider;
  
  public static X509Certificate generateCertificate( final KeyPair keys, final String userName ) {
    return getProvider().generateCertificate( keys, userName );
  }
  
  public static X509Certificate generateCertificate( final KeyPair keys, final X500Principal subjectDn ) {
    return getProvider().generateCertificate( keys, subjectDn );
  }
  
  public static X509Certificate generateCertificate( final KeyPair keys, final X500Principal subjectDn, final X500Principal signer, final PrivateKey signingKey ) {
    return getProvider().generateCertificate( keys, subjectDn, signer, signingKey );
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateCertificateCode(java.lang.String)
   */
  public static String generateCertificateCode( final String userName ) {
    return getProvider().generateCertificateCode( userName );
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateConfirmationCode(java.lang.String)
   */
  public static String generateConfirmationCode( final String userName ) {
    return getProvider().generateConfirmationCode( userName );
  }
  
  /**
   * @param password
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateHashedPassword(java.lang.String)
   */
  public static String generateHashedPassword( final String password ) {
    return getProvider().generateHashedPassword( password );
  }
  
  public static KeyPair generateKeyPair( ) {
    return getProvider().generateKeyPair( );
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateQueryId(java.lang.String)
   */
  public static String generateQueryId( final String userName ) {
    return getProvider().generateQueryId( userName );
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateSecretKey(java.lang.String)
   */
  public static String generateSecretKey( final String userName ) {
    return getProvider().generateSecretKey( userName );
  }
  
  public static X509Certificate generateServiceCertificate( final KeyPair keys, final String userName ) {
    return getProvider().generateServiceCertificate( keys, userName );
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateSessionToken(java.lang.String)
   */
  public static String generateSessionToken( final String userName ) {
    return getProvider().generateSessionToken( userName );
  }
  
  /**
   * @param input
   * @param hash
   * @param randomize
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#getDigestBase64(java.lang.String, com.eucalyptus.auth.crypto.Digest, boolean)
   */
  public static String getDigestBase64( final String input, final Digest hash, final boolean randomize ) {
    return getProvider().getDigestBase64( input, hash, randomize );
  }
  
  public static <T> EntityWrapper<T> getEntityWrapper( ) {
    return new EntityWrapper<T>( Credentials.DB_NAME );
  }
  
  public static CryptoProvider getProvider( ) {
    //TODO FIXME TODO BROKEN FAIL: discover this at bootstrap time.
    try {
      Class.forName( "com.eucalyptus.auth.crypto.DefaultCryptoProvider" );
    } catch ( ClassNotFoundException e ) {
    }
    //TODO FIXME TODO BROKEN FAIL: discover this at bootstrap time.
    return provider;
  }
  
  public static void setProvider( final CryptoProvider newProvider ) {
    if ( provider == null ) {
      synchronized ( Credentials.class ) {
        LOG.info( "Setting the crypto provider to: " + newProvider.getClass( ) );
        provider = newProvider;
      }
    }
  }
  
  public static void writePem( final String fileName, final Object securityToken ) {
    PEMWriter privOut = null;
    try {
      privOut = new PEMWriter( new FileWriter( fileName ) );
      privOut.writeObject( securityToken );
      privOut.close( );
    } catch ( final IOException e ) {
      LOG.error( e, e );
    }
  }
  
}
