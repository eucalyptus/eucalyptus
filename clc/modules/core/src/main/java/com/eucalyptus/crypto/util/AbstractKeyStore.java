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

package com.eucalyptus.crypto.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

public abstract class AbstractKeyStore implements com.eucalyptus.crypto.KeyStore {
  
  public static AbstractKeyStore getGenericKeystore( final String fileName, final String password, final String format ) throws IOException, GeneralSecurityException {
    return new GenericKeyStore( fileName, password, format );
  }
  
  private static Logger          LOG = Logger.getLogger( AbstractKeyStore.class );
  
  private java.security.KeyStore keyStore;
  private final String           fileName;
  private final String           password;
  private final String           format;
  
  public AbstractKeyStore( final String fileName, final String password, final String format ) throws GeneralSecurityException, IOException {
    this.password = password;
    this.fileName = fileName;
    this.format = format;
    this.init( );
  }
  
  @Override
  public KeyPair getKeyPair( final String alias, final String password ) throws GeneralSecurityException {
    final Certificate cert = this.keyStore.getCertificate( alias );
    final PrivateKey privateKey = ( PrivateKey ) this.keyStore.getKey( alias, password.toCharArray( ) );
    final KeyPair kp = new KeyPair( cert.getPublicKey( ), privateKey );
    return kp;
  }
  
  @Override
  public abstract boolean check( ) throws GeneralSecurityException;
  
  @Override
  public boolean containsEntry( final String alias ) {
    try {
      if ( ( X509Certificate ) this.keyStore.getCertificate( alias ) != null ) {
        return true;
      }
    } catch ( final KeyStoreException e ) {}
    return false;
  }
  
  @Override
  public X509Certificate getCertificate( final String alias ) throws GeneralSecurityException {
    return ( X509Certificate ) this.keyStore.getCertificate( alias );
  }

  @Override
  public List<X509Certificate> getCertificateChain(final String alias) throws GeneralSecurityException {
    List<X509Certificate> result = Lists.newArrayList();
    Certificate[] certChain = this.keyStore.getCertificateChain(alias);
    for (Certificate cert: certChain) {
      if (cert instanceof X509Certificate) {
        result.add((X509Certificate) cert);
      }
    }
    return result;
  }
  
  @Override
  public Key getKey( final String alias, final String password ) throws GeneralSecurityException {
    return this.keyStore.getKey( alias, password.toCharArray( ) );
  }
  
  @Override
  public String getCertificateAlias( final String certPem ) throws GeneralSecurityException {
    final X509Certificate cert = AbstractKeyStore.pemToX509( certPem );
    return this.keyStore.getCertificateAlias( cert );
  }
  
  public static X509Certificate pemToX509( final String certPem ) throws CertificateException, NoSuchProviderException {
    final CertificateFactory certificatefactory = CertificateFactory.getInstance( "X.509", "BC" );
    final X509Certificate cert = ( X509Certificate ) certificatefactory.generateCertificate( new ByteArrayInputStream( certPem.getBytes( ) ) );
    return cert;
  }
  
  @Override
  public String getCertificateAlias( final X509Certificate cert ) throws GeneralSecurityException {
    final String alias = this.keyStore.getCertificateAlias( cert );
    if ( alias == null ) {
      throw new GeneralSecurityException( "No Such Certificate!" );
    }
    return alias;
  }
  
  @Override
  public void addCertificate( final String alias, final X509Certificate cert ) throws IOException, GeneralSecurityException {
    LOG.info( String.format( "Adding certificate %10s %s to %s", alias, cert, this.fileName ) );
    this.keyStore.setCertificateEntry( alias, cert );
    this.store( );
  }
  
  @Override
  public void addKeyPair( final String alias, final X509Certificate cert, final PrivateKey privateKey, final String keyPassword ) throws IOException, GeneralSecurityException {
    this.keyStore.setKeyEntry( alias, privateKey, keyPassword.toCharArray( ), new Certificate[] { cert } );
    this.store( );
  }
  
  @Override
  public void store( ) throws IOException, GeneralSecurityException {
    LOG.info( "Writing back keystore: " + this.fileName );
    final FileOutputStream fileOutputStream = new FileOutputStream( this.fileName );
    this.keyStore.store( fileOutputStream, this.password.toCharArray( ) );
    fileOutputStream.close( );
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
  
  @Override
  public List<String> getAliases( ) throws KeyStoreException {
    return Collections.list( this.keyStore.aliases( ) );
  }
  
  @Override
  public String getFileName( ) {
    return this.fileName;
  }
  
  @Override
  public void remove( final String alias ) {
    try {
      this.keyStore.deleteEntry( alias );
      this.store( );
    } catch ( final KeyStoreException ex ) {
      LOG.error( ex, ex );
    } catch ( final IOException ex ) {
      LOG.error( ex, ex );
    } catch ( final GeneralSecurityException ex ) {
      LOG.error( ex, ex );
    }
  }
  
  @Override
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
  
  @Override
  public InputStream getAsInputStream( ) throws FileNotFoundException {
    return new FileInputStream( this.fileName );
  }
  
  protected java.security.KeyStore getKeyStore( ) {
    return this.keyStore;
  }
  
  @Override
  public List<X509Certificate> getAllCertificates() throws KeyStoreException {
    List<X509Certificate> results = Lists.newArrayList( );
    for ( String alias : Collections.list( this.keyStore.aliases( ) ) ) {
      if ( this.keyStore.isCertificateEntry( alias ) ) {
        Certificate cert = this.keyStore.getCertificate( alias );
        if ( cert instanceof X509Certificate ) {
          results.add( ( X509Certificate ) cert );
        }
      }
    }
    return results;
  }
  
}
