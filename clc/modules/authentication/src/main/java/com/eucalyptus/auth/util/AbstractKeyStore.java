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
package com.eucalyptus.auth.util;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public abstract class AbstractKeyStore {

  public static AbstractKeyStore getGenericKeystore( final String fileName, final String password, final String format ) throws IOException, GeneralSecurityException {
    return new GenericKeyStore( fileName, password, format );
  }

  private static Logger LOG = Logger.getLogger( AbstractKeyStore.class );


  private KeyStore      keyStore;
  private final String  fileName;
  private final String  password;
  private final String  format;

  public AbstractKeyStore( final String fileName, final String password, final String format ) throws GeneralSecurityException, IOException {
    this.password = password;
    this.fileName = fileName;
    this.format = format;
    this.init( );
  }

  public KeyPair getKeyPair( final String alias, final String password ) throws GeneralSecurityException {
    final Certificate cert = this.keyStore.getCertificate( alias );
    final PrivateKey privateKey = ( PrivateKey ) this.keyStore.getKey( alias, password.toCharArray( ) );
    final KeyPair kp = new KeyPair( cert.getPublicKey( ), privateKey );
    return kp;
  }

  public abstract boolean check( ) throws GeneralSecurityException;

  public boolean containsEntry( final String alias ) {
    try {
      if ( ( X509Certificate ) this.keyStore.getCertificate( alias ) != null ) { return true; }
    } catch ( final KeyStoreException e ) {
    }
    return false;
  }

  public X509Certificate getCertificate( final String alias ) throws GeneralSecurityException {
    return ( X509Certificate ) this.keyStore.getCertificate( alias );
  }

  public Key getKey( final String alias, final String password ) throws GeneralSecurityException {
    return this.keyStore.getKey( alias, password.toCharArray( ) );
  }

  public String getCertificateAlias( final String certPem ) throws GeneralSecurityException {
    final X509Certificate cert = AbstractKeyStore.pemToX509( certPem );
    return this.keyStore.getCertificateAlias( cert );
  }

  public static X509Certificate pemToX509( final String certPem ) throws CertificateException, NoSuchProviderException {
    final CertificateFactory certificatefactory = CertificateFactory.getInstance( "X.509", "BC" );
    final X509Certificate cert = ( X509Certificate ) certificatefactory.generateCertificate( new ByteArrayInputStream( certPem.getBytes( ) ) );
    return cert;
  }

  public String getCertificateAlias( final X509Certificate cert ) throws GeneralSecurityException {
    final String alias = this.keyStore.getCertificateAlias( cert );
    if ( alias == null ) { throw new GeneralSecurityException( "No Such Certificate!" ); }
    return alias;
  }

  public void addCertificate( final String alias, final X509Certificate cert ) throws IOException, GeneralSecurityException {
    AbstractKeyStore.LOG.info( String.format( "Adding certificate %10s %s to %s", alias, cert, this.fileName ) );
    this.keyStore.setCertificateEntry( alias, cert );
    this.store( );
  }

  public void addKeyPair( final String alias, final X509Certificate cert, final PrivateKey privateKey, final String keyPassword ) throws IOException, GeneralSecurityException {
    this.keyStore.setKeyEntry( alias, privateKey, keyPassword.toCharArray( ), new Certificate[] { cert } );
    this.store( );
  }

  public void store( ) throws IOException, GeneralSecurityException {
    AbstractKeyStore.LOG.info( "Writing back keystore: " + this.fileName );
    FileOutputStream fileOutputStream = new FileOutputStream( this.fileName );
	this.keyStore.store( fileOutputStream, this.password.toCharArray( ) );
	fileOutputStream.close();
  }

  private void init( ) throws IOException, GeneralSecurityException {
    this.keyStore = KeyStore.getInstance( this.format, "BC" );
    if ( ( new File( this.fileName ) ).exists( ) ) {
      final FileInputStream fin = new FileInputStream( this.fileName );
      this.keyStore.load( fin, this.password.toCharArray( ) );
      fin.close( );
    } else {
      this.keyStore.load( null, this.password.toCharArray( ) );
    }
  }

  public List<String> getAliases( ) throws KeyStoreException {
    final List<String> aliasList = new ArrayList<String>( );
    final Enumeration<String> aliases = this.keyStore.aliases( );
    while ( aliases.hasMoreElements( ) ) {
      aliasList.add( aliases.nextElement( ) );
    }
    return aliasList;
  }

  public String getFileName( ) {
    return this.fileName;
  }

  public void remove( ) {
    ( new File( this.fileName ) ).delete( );
  }

  static class GenericKeyStore extends AbstractKeyStore {

    private GenericKeyStore( final String fileName, final String password, final String format ) throws IOException, GeneralSecurityException {
      super( fileName, password, format );
    }

    @Override
    public boolean check( ) throws KeyStoreException {
      throw new RuntimeException( "A GenericKeyStore does not have the notion of being checked for correctness." );
    }
  }

  public InputStream getAsInputStream() throws FileNotFoundException {
    return new FileInputStream( this.fileName );
  }

  public KeyStore getKeyStore( ) {
    return keyStore;
  }

}
