/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.UrlBase64;
import com.eucalyptus.util.CompatSupplier;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * Facade for helpers and a set of generator methods providing non-trivial random tokens.
 */
public class Crypto {
  private static final Logger LOG = Logger.getLogger( Crypto.class );
  private static final BaseSecurityProvider DUMMY = new BaseSecurityProvider( ) {};
  private static final ConcurrentMap<Class, BaseSecurityProvider> providers = new ConcurrentHashMap<>( );
  static {
    BaseSecurityProvider provider;
    try {
      Class provClass = ClassLoader.getSystemClassLoader().loadClass( "com.eucalyptus.crypto.DefaultCryptoProvider" );
      provider = ( BaseSecurityProvider ) provClass.newInstance( );
    } catch ( Exception t ) {
      LOG.debug( t, t );
      provider = DUMMY;
    }
    providers.put( CertificateProvider.class, provider );
    providers.put( HmacProvider.class, provider );
    providers.put( CryptoProvider.class, provider );
  }
  private static final CompatSupplier<SecureRandom> secureRandomSupplier = CompatSupplier.of( Suppliers.memoizeWithExpiration( new Supplier<SecureRandom>() {
    private final String secureRandomAlgorithm = System.getProperty( "euca.crypto.random.algorithm", "SHA1PRNG" );
    private final String secureRandomProvider = System.getProperty( "euca.crypto.random.provider", "SUN" );
    @Override
    public SecureRandom get() {
      try {
        return secureRandomProvider.isEmpty() ?
            SecureRandom.getInstance( secureRandomAlgorithm ) :
            SecureRandom.getInstance( secureRandomAlgorithm, secureRandomProvider );
      } catch (GeneralSecurityException e) {
        throw Exceptions.toUndeclared(e);
      }
    }
  }, 15, TimeUnit.MINUTES ) );

  /**
   * @see com.eucalyptus.crypto.CryptoProvider#generateHashedPassword(java.lang.String)
   */
  @SuppressWarnings( "WeakerAccess" )
  public static String generateHashedPassword( final String password ) {
    return Crypto.getCryptoProvider( ).generateHashedPassword( password );
  }
  
  /**
   * @see com.eucalyptus.crypto.CryptoProvider#generateSessionToken()
   */
  public static String generateSessionToken() {
    return Crypto.getCryptoProvider( ).generateSessionToken();
  }

  /**
   * @see com.eucalyptus.crypto.CryptoProvider#generateAlphanumericId(int)
   */
  public static String generateAlphanumericId( final int length ) {
    return Crypto.getCryptoProvider().generateAlphanumericId( length );
  }

  /**
   * @see com.eucalyptus.crypto.CryptoProvider#generateSecretKey()
   */
  public static String generateSecretKey() {
    return Crypto.getCryptoProvider().generateSecretKey();
  }

  /**
   * @see com.eucalyptus.crypto.CryptoProvider#getDigestBase64(java.lang.String, com.eucalyptus.crypto.Digest)
   */
  public static String getDigestBase64( final String input, final Digest hash ) {
    return Crypto.getCryptoProvider( ).getDigestBase64( input, hash );
  }
  
  /**
   * @see com.eucalyptus.crypto.CryptoProvider#generateId(String)
   */
  public static String generateId( final String prefix ) {
    return Crypto.getCryptoProvider( ).generateId( prefix );
  }

  public static String generateLongId( final String prefix ) {
    return Crypto.getCryptoProvider( ).generateLongId( prefix );
  }

  public static CertificateProvider getCertificateProvider( ) {
    return (CertificateProvider) providers.get( CertificateProvider.class );
  }

  @SuppressWarnings( "WeakerAccess" )
  public static HmacProvider getHmacProvider( ) {
    return (HmacProvider) providers.get( HmacProvider.class );
  }

  @SuppressWarnings( "WeakerAccess" )
  public static CryptoProvider getCryptoProvider( ) {
    return (CryptoProvider) providers.get( CryptoProvider.class );
  }

  @SuppressWarnings( "WeakerAccess" )
  public static String generateLinuxSaltedPassword( final String password ) {
    return Crypto.getCryptoProvider( ).generateLinuxSaltedPassword( password );
  }

  @SuppressWarnings( "WeakerAccess" )
  public static boolean verifyLinuxSaltedPassword( final String clear, final String hashed ) {
    return Crypto.getCryptoProvider( ).verifyLinuxSaltedPassword( clear, hashed );
  }
  
  /**
   * verifyPassword checks if a hashed password matches its clear text form,
   * using Linux-compatible salted password hash;
   * if not match, fall back to original non-salted hash.
   */
  public static boolean verifyPassword( final String clear, final String hashed ) {
    try {
      // Try salted hash first
      return !Strings.isNullOrEmpty(hashed) && Crypto.verifyLinuxSaltedPassword( clear, hashed );
    } catch ( Exception e ) {
      // Fall back to old password hash
      if ( clear != null ) {
        return hashed.equals( Crypto.generateHashedPassword( clear ) );
      }
    }
    return false;
  }
  
  /**
   * A gateway for creating the encrypted password.
   */
  public static String generateEncryptedPassword( final String password ) {
    // Use Linux-compatible salted password
    return Crypto.generateLinuxSaltedPassword( password );
  }

  /**
   * Get a supplier for secure random.
   *
   * <p>The returned supplier may be cached. A secure random instance returned
   * from the provider should not be cached long periods.</p>
   *
   * @return The supplier
   */
  @Nonnull
  public static CompatSupplier<SecureRandom> getSecureRandomSupplier() {
    return secureRandomSupplier;
  }

  public static String getRandom( int size ) {
    SecureRandom random = getSecureRandomSupplier( ).get( );
    byte[] randomBytes = new byte[size];
    random.nextBytes( randomBytes );
    return new String( UrlBase64.encode( randomBytes ) );
  }
}
