/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.crypto.util;

import static java.lang.System.getProperty;
import static com.eucalyptus.crypto.util.SslUtils.getEnabledCipherSuites;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.toArray;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedKeyManager;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.PropertyChangeListeners;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.IO;
import com.eucalyptus.util.Pair;
import com.google.common.base.Objects;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;
import com.sun.net.ssl.internal.ssl.X509ExtendedTrustManager;

@ConfigurableClass( root = "bootstrap.webservices.ssl",
                    description = "Parameters controlling the SSL configuration for the web services endpoint." )
public class SslSetup {
  private static final Logger LOG             = Logger.getLogger( SslSetup.class );
  private static final String PROTOCOL        = "TLS";
  private static SSLContext   SERVER_CONTEXT  = null;
  private static SSLContext   CLIENT_CONTEXT  = null;
  @ConfigurableField( description = "Alias of the certificate entry in euca.p12 to use for SSL for webservices.",
                      changeListener = SslCertChangeListener.class )
  public static String        SERVER_ALIAS    = ComponentIds.lookup( Eucalyptus.class ).name( );
  @ConfigurableField( description = "Password of the private key corresponding to the specified certificate for SSL for webservices.",
                      type = ConfigurableFieldType.KEYVALUEHIDDEN,
                      changeListener = SslPasswordChangeListener.class )
  public static String        SERVER_PASSWORD = ComponentIds.lookup( Eucalyptus.class ).name( );
  @ConfigurableField( description = "SSL ciphers for webservices." )
  public static String        SERVER_SSL_CIPHERS = "RSA:DSS:ECDSA:+RC4:+3DES:TLS_EMPTY_RENEGOTIATION_INFO_SCSV:!NULL:!EXPORT:!EXPORT1024:!MD5:!DES";
  @ConfigurableField( description = "SSL protocols for webservices." )
  public static String        SERVER_SSL_PROTOCOLS = "SSLv2Hello,TLSv1,TLSv1.1,TLSv1.2";
  @ConfigurableField( description = "Use default CAs with SSL for external use.", changeListener = PropertyChangeListeners.IsBoolean.class )
  public static Boolean       USER_SSL_DEFAULT_CAS = true;
  @ConfigurableField( description = "SSL ciphers for external use." )
  public static String        USER_SSL_CIPHERS = "RSA:DSS:ECDSA:+RC4:+3DES:TLS_EMPTY_RENEGOTIATION_INFO_SCSV:!NULL:!EXPORT:!EXPORT1024:!MD5:!DES";
  @ConfigurableField( description = "SSL protocols for external use." )
  public static String        USER_SSL_PROTOCOLS = "SSLv2Hello,TLSv1,TLSv1.1,TLSv1.2";
  @ConfigurableField( description = "SSL hostname validation for external use.", changeListener = PropertyChangeListeners.IsBoolean.class )
  public static Boolean       USER_SSL_ENABLE_HOSTNAME_VERIFICATION = true;

  public static class SslCertChangeListener implements PropertyChangeListener<String> {
    
    @Override
    public void fireChange( ConfigurableProperty t, String newValue ) throws ConfigurablePropertyException {
      if ( SERVER_ALIAS != null && !SERVER_ALIAS.equals( newValue ) ) {
        try {
          String oldValue = SERVER_ALIAS;
          SSLContext newContext = createServerContext( );
          SERVER_ALIAS = newValue;
          SERVER_CONTEXT = newContext;
        } catch ( Exception ex ) {
          throw new ConfigurablePropertyException( ex );
        }
      }
    }
    
  }
  
  public static class SslPasswordChangeListener implements PropertyChangeListener<String> {
    
    @Override
    public void fireChange( ConfigurableProperty t, String newValue ) throws ConfigurablePropertyException {
      if ( SERVER_PASSWORD != null && !SERVER_PASSWORD.equals( newValue ) ) {
        try {
          String oldValue = SERVER_PASSWORD;
          SSLContext newContext = createServerContext( );
          SERVER_PASSWORD = newValue;
          SERVER_CONTEXT = newContext;
        } catch ( Exception ex ) {
          throw new ConfigurablePropertyException( ex );
        }
      }
    }
    
  }
  
  static {
    BCSslSetup.initBouncyCastleDHParams();
    SSLContext serverContext;
    SSLContext clientContext;
    System.setProperty( "javax.net.ssl.trustStore", SubDirectory.KEYS.toString( ) + File.separator + "euca.p12" );
    System.setProperty( "javax.net.ssl.keyStore", SubDirectory.KEYS.toString( ) + File.separator + "euca.p12" );
    System.setProperty( "javax.net.ssl.trustStoreType", "PKCS12" );
    System.setProperty( "javax.net.ssl.keyStoreType", "PKCS12" );
    System.setProperty( "javax.net.ssl.trustStorePassword", ComponentIds.lookup( Eucalyptus.class ).name( ) );
    System.setProperty( "javax.net.ssl.keyStorePassword", ComponentIds.lookup( Eucalyptus.class ).name( ) );
//    System.setProperty( "javax.net.debug", "ssl" );//set this to "ssl" for debugging.
    try {
      serverContext = createServerContext( );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new Error( "Failed to initialize the server-side SSLContext", e );
    }
    
    try {
      clientContext = SSLContext.getInstance( PROTOCOL );
      clientContext.init( SslSetup.ClientKeyManager.getKeyManagers( ), SslSetup.ClientTrustManager.getTrustManagers( ), null );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new Error( "Failed to initialize the client-side SSLContext", e );
    }
    
    SERVER_CONTEXT = serverContext;
    CLIENT_CONTEXT = clientContext;
  }
  
  private static SSLContext createServerContext( ) throws NoSuchAlgorithmException, KeyManagementException {
    SSLContext serverContext;
    serverContext = SSLContext.getInstance( PROTOCOL );
    serverContext.init( SslSetup.ServerKeyManager.getKeyManagers( ), SslSetup.ServerTrustManager.getTrustManagers( ), null );
    return serverContext;
  }
  
  public static SSLContext getServerContext( ) {
    return SERVER_CONTEXT;
  }
  
  public static SSLEngine getServerEngine( ) {//TODO:GRZE: @Configurability
    final SSLEngine engine = SslSetup.getServerContext( ).createSSLEngine( );
    engine.setUseClientMode( false );
    engine.setWantClientAuth( false );
    engine.setNeedClientAuth( false );
    engine.setEnabledProtocols( SslUtils.getEnabledProtocols( SERVER_SSL_PROTOCOLS, engine.getSupportedProtocols( ) ) );
    engine.setEnabledCipherSuites( SslUtils.getEnabledCipherSuites( SERVER_SSL_CIPHERS, engine.getSupportedCipherSuites( ) ) );
    return engine;
  }

  /**
   * Configure an unconnected HttpsURLConnection for trust.
   */
  public static URLConnection configureHttpsUrlConnection( final URLConnection urlConnection ) {
    if ( urlConnection instanceof HttpsURLConnection ) {
      final HttpsURLConnection httpsURLConnection = (HttpsURLConnection) urlConnection;
      httpsURLConnection.setHostnameVerifier(
          Objects.firstNonNull( USER_SSL_ENABLE_HOSTNAME_VERIFICATION, Boolean.TRUE ) ?
            new BrowserCompatHostnameVerifier( ) :
            new AllowAllHostnameVerifier( ) );
      httpsURLConnection.setSSLSocketFactory( UserSSLSocketFactory.get( ) );
    }
    return urlConnection;
  }
  
  public static SSLContext getClientContext( ) {
    return CLIENT_CONTEXT;
  }

  static class ClientKeyManager extends KeyManagerFactorySpi {
    private static KeyManager singleton = new ClientPKCS12KeyManager( );
    
    public static KeyManager[] getKeyManagers( ) {
      return new KeyManager[] { singleton };
    }
    
    @Override
    protected KeyManager[] engineGetKeyManagers( ) {
      return new KeyManager[] { singleton };
    }
    
    @Override
    protected void engineInit( ManagerFactoryParameters spec ) throws InvalidAlgorithmParameterException {}
    
    @Override
    protected void engineInit( KeyStore ks, char[] password ) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {}
    
    static class ClientPKCS12KeyManager extends X509ExtendedKeyManager {
      
      @Override
      public String chooseClientAlias( String[] arg0, Principal[] arg1, Socket arg2 ) {
        return ComponentIds.lookup( Eucalyptus.class ).name( );
      }
      
      @Override
      public String chooseServerAlias( String arg0, Principal[] arg1, Socket arg2 ) {
        return ComponentIds.lookup( Eucalyptus.class ).name( );
      }
      
      @Override
      public X509Certificate[] getCertificateChain( String arg0 ) {
        if ( ComponentIds.lookup( Eucalyptus.class ).name( ).equals( arg0 ) ) {
          return trustedCerts;
        } else {
          return null;
        }
      }
      
      @Override
      public String[] getClientAliases( String arg0, Principal[] arg1 ) {
        return new String[] { ComponentIds.lookup( Eucalyptus.class ).name( ) };
      }
      
      @Override
      public PrivateKey getPrivateKey( String arg0 ) {
        if ( ComponentIds.lookup( Eucalyptus.class ).name( ).equals( arg0 ) ) {
          return trustedKey;
        } else {
          return null;
        }
      }
      
      @Override
      public String[] getServerAliases( String arg0, Principal[] arg1 ) {
        return new String[] { ComponentIds.lookup( Eucalyptus.class ).name( ) };
      }
      
      @Override
      public String chooseEngineClientAlias( String[] keyType, Principal[] issuers, SSLEngine engine ) {
        return ComponentIds.lookup( Eucalyptus.class ).name( );
      }
      
      @Override
      public String chooseEngineServerAlias( String keyType, Principal[] issuers, SSLEngine engine ) {
        return ComponentIds.lookup( Eucalyptus.class ).name( );
      }
      
    }
    
  }
  
  static class ServerKeyManager extends KeyManagerFactorySpi {
    private static KeyManager singleton = new ServerPKCS12KeyManager( );
    
    public static KeyManager[] getKeyManagers( ) {
      return new KeyManager[] { singleton };
    }
    
    @Override
    protected KeyManager[] engineGetKeyManagers( ) {
      return new KeyManager[] { singleton };
    }
    
    @Override
    protected void engineInit( ManagerFactoryParameters spec ) throws InvalidAlgorithmParameterException {}
    
    @Override
    protected void engineInit( KeyStore ks, char[] password ) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {}
    
    static class ServerPKCS12KeyManager extends X509ExtendedKeyManager {
      
      @Override
      public String chooseClientAlias( String[] arg0, Principal[] arg1, Socket arg2 ) {
        return SslSetup.SERVER_ALIAS;
      }
      
      @Override
      public String chooseServerAlias( String arg0, Principal[] arg1, Socket arg2 ) {
        return SslSetup.SERVER_ALIAS;
      }
      
      @Override
      public X509Certificate[] getCertificateChain( String arg0 ) {
        if ( SslSetup.SERVER_ALIAS.equals( arg0 ) ) {
          return memoizedServerCertSupplier.get( );
        } else {
          return null;
        }
      }
      
      @Override
      public String[] getClientAliases( String arg0, Principal[] arg1 ) {
        return new String[] { SslSetup.SERVER_ALIAS };
      }
      
      @Override
      public PrivateKey getPrivateKey( String arg0 ) {
        if ( SslSetup.SERVER_ALIAS.equals( arg0 ) ) {
          return memoizedPrivateKeySupplier.get( );
        } else {
          return null;
        }
      }
      
      @Override
      public String[] getServerAliases( String arg0, Principal[] arg1 ) {
        return new String[] { SslSetup.SERVER_ALIAS };
      }
      
      @Override
      public String chooseEngineClientAlias( String[] keyType, Principal[] issuers, SSLEngine engine ) {
        return SslSetup.SERVER_ALIAS;
      }
      
      @Override
      public String chooseEngineServerAlias( String keyType, Principal[] issuers, SSLEngine engine ) {
        return SslSetup.SERVER_ALIAS;
      }
      
    }
    
  }
  
  private static final Supplier<PrivateKey>        serverPrivateKeySupplier   = new Supplier<PrivateKey>( ) {
                                                                                
                                                                                @Override
                                                                                public PrivateKey get( ) {
                                                                                  try {
                                                                                    return SystemCredentials.getKeyStore( ).getKeyPair(
                                                                                      SslSetup.SERVER_ALIAS,
                                                                                      SslSetup.SERVER_ALIAS ).getPrivate( );
                                                                                  } catch ( GeneralSecurityException ex ) {
                                                                                    LOG.error( ex, ex );
                                                                                    return null;
                                                                                  }
                                                                                }
                                                                              };
  private static final Supplier<X509Certificate[]> serverCertSupplier         = new Supplier<X509Certificate[]>( ) {
                                                                                
                                                                                @Override
                                                                                public X509Certificate[] get( ) {
                                                                                  X509Certificate[] certs = ObjectArrays.newArray( X509Certificate.class, 1 );
                                                                                  try {
                                                                                    certs[0] = SystemCredentials.getKeyStore( ).getCertificate(
                                                                                      SslSetup.SERVER_ALIAS );
                                                                                    return certs;
                                                                                  } catch ( GeneralSecurityException ex ) {
                                                                                    LOG.error( ex, ex );
                                                                                    return certs;
                                                                                  }
                                                                                }
                                                                              };
  private static Supplier<PrivateKey>              memoizedPrivateKeySupplier = Suppliers.memoizeWithExpiration( serverPrivateKeySupplier, 15l, TimeUnit.SECONDS );
  private static Supplier<X509Certificate[]>       memoizedServerCertSupplier = Suppliers.memoizeWithExpiration( serverCertSupplier, 15l, TimeUnit.SECONDS );
  private static PrivateKey                        trustedKey                 = getTrustedKey( );
  private static X509Certificate[]                 trustedCerts               = getTrustedCertificates( );
  
  private static X509Certificate[] getTrustedCertificates( ) {
    try {
      synchronized ( SslSetup.class ) {
        if ( trustedCerts == null ) {
          trustedCerts = SystemCredentials.getKeyStore( ).getAllCertificates( ).toArray( new X509Certificate[0] );
        }
        return trustedCerts;
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new RuntimeException( e );
    }
  }
  
  private static PrivateKey getTrustedKey( ) {
    try {
      synchronized ( SslSetup.class ) {
        if ( trustedKey == null ) {
          trustedKey = SystemCredentials.getKeyStore( ).getKeyPair(
            ComponentIds.lookup( Eucalyptus.class ).name( ),
            ComponentIds.lookup( Eucalyptus.class ).name( ) ).getPrivate( );
        }
        return trustedKey;
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new RuntimeException( e );
    }
  }

  public static class ClientTrustManager extends TrustManagerFactorySpi {
    
    private static final TrustManager singleton = new SimpleX509TrustManager( );
    
    public static TrustManager[] getTrustManagers( ) {
      return new TrustManager[] { singleton };
    }
    
    @Override
    protected TrustManager[] engineGetTrustManagers( ) {
      return getTrustManagers( );
    }
    
    @Override
    protected void engineInit( KeyStore keystore ) throws KeyStoreException {}
    
    @Override
    protected void engineInit( ManagerFactoryParameters managerFactoryParameters ) throws InvalidAlgorithmParameterException {}
    
    public static class SimpleX509TrustManager extends X509ExtendedTrustManager {
      
      @Override
      public void checkClientTrusted( X509Certificate[] arg0, String arg1 ) throws CertificateException {}
      
      @Override
      public void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException {}
      
      @Override
      public X509Certificate[] getAcceptedIssuers( ) {
        return trustedCerts;
      }
      
      @Override
      public void checkClientTrusted( X509Certificate[] arg0, String arg1, String arg2, String arg3 ) throws CertificateException {}
      
      @Override
      public void checkServerTrusted( X509Certificate[] arg0, String arg1, String arg2, String arg3 ) throws CertificateException {}
    }
    
  }
  
  static class ServerTrustManager extends TrustManagerFactorySpi {
    
    private static final TrustManager singleton = new SimpleX509TrustManager( );
    
    public static TrustManager[] getTrustManagers( ) {
      return new TrustManager[] { singleton };
    }
    
    @Override
    protected TrustManager[] engineGetTrustManagers( ) {
      return getTrustManagers( );
    }
    
    @Override
    protected void engineInit( KeyStore keystore ) throws KeyStoreException {}
    
    @Override
    protected void engineInit( ManagerFactoryParameters managerFactoryParameters ) throws InvalidAlgorithmParameterException {}
    
    public static class SimpleX509TrustManager extends X509ExtendedTrustManager {
      
      @Override
      public void checkClientTrusted( X509Certificate[] arg0, String arg1 ) throws CertificateException {}
      
      @Override
      public void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException {}
      
      @Override
      public X509Certificate[] getAcceptedIssuers( ) {
        return serverCertSupplier.get( );
      }
      
      @Override
      public void checkClientTrusted( X509Certificate[] arg0, String arg1, String arg2, String arg3 ) throws CertificateException {}
      
      @Override
      public void checkServerTrusted( X509Certificate[] arg0, String arg1, String arg2, String arg3 ) throws CertificateException {}
    }
    
  }

  public static class UserSSLSocketFactory extends SSLSocketFactoryWrapper {
    private static final String PROP_USER_SSL_PROVIDER = "com.eucalyptus.crypto.util.userSslProvider";
    private static final String PROP_USER_SSL_PROTOCOL = "com.eucalyptus.crypto.util.userSslProtocol";
    private static final String PROP_USER_SSL_TRUSTSTORE_PASSWORD = "com.eucalyptus.crypto.util.userSslTrustStorePassword";
    private static final String PROP_USER_SSL_TRUSTSTORE_TYPE = "com.eucalyptus.crypto.util.userSslTrustStoreType";
    private static final String PROP_USER_SSL_TRUSTSTORE_PATH = "com.eucalyptus.crypto.util.userSslTrustStorePath";
    private static final String DEFAULT_PROTOCOL = "TLS";
    private static final String DEFAULT_TRUSTSTORE_PASSWORD = "changeit";
    private static final String DEFAULT_TRUSTSTORE_PATH = "lib/security/cacerts";
    private final List<String> cipherSuites;
    private final static AtomicReference<Pair<Long,KeyStore>> sslTrustStore = new AtomicReference<>( );

    private static final Supplier<UserSSLSocketFactory> supplier = new Supplier<UserSSLSocketFactory>( ){
      @Override
      public UserSSLSocketFactory get( ) {
        return new UserSSLSocketFactory( );
      }
    };

    private static final Supplier<UserSSLSocketFactory> memoizedSupplier =
        Suppliers.memoizeWithExpiration( supplier, 60l, TimeUnit.SECONDS );

    public static UserSSLSocketFactory get( ) {
      return memoizedSupplier.get( );
    }

    public UserSSLSocketFactory( ) {
      super( Suppliers.ofInstance( buildDelegate( ) ) );
      this.cipherSuites =
          ImmutableList.copyOf( getEnabledCipherSuites( USER_SSL_CIPHERS, getSupportedCipherSuites( ) ) );
    }

    @Override
    protected Socket notifyCreated(final Socket socket) {
      if ( socket instanceof SSLSocket ) {
        final SSLSocket sslSocket = (SSLSocket) socket;
        sslSocket.setEnabledCipherSuites( toArray( cipherSuites, String.class ) );
        sslSocket.setEnabledProtocols( SslUtils.getEnabledProtocols( USER_SSL_PROTOCOLS, sslSocket.getSupportedProtocols( ) ) );
      }
      return socket;
    }

    private static KeyStore getTrustStore( final File trustStore ) throws GeneralSecurityException, IOException {
      final Pair<Long,KeyStore> currentTrustStore = sslTrustStore.get( );
      InputStream trustStoreIn = null;
      if ( currentTrustStore != null && currentTrustStore.getLeft( ) == trustStore.lastModified( ) ) {
        return currentTrustStore.getRight( );
      } else try {
        final String trustStoreType = getProperty( PROP_USER_SSL_TRUSTSTORE_TYPE, KeyStore.getDefaultType( ) );
        final String trustStorePassword = getProperty( PROP_USER_SSL_TRUSTSTORE_PASSWORD, DEFAULT_TRUSTSTORE_PASSWORD );
        final KeyStore userTrustStore = KeyStore.getInstance( trustStoreType );
        userTrustStore.load( trustStoreIn = new FileInputStream( trustStore ), trustStorePassword.toCharArray() );
        sslTrustStore.set( Pair.pair( trustStore.lastModified( ), userTrustStore ) );
        return userTrustStore;
      } finally {
        IO.close( trustStoreIn );
      }
    }

    private static List<TrustManager> trustManagersForStore( final KeyStore trustStore ) throws GeneralSecurityException {
      final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance( "PKIX", "SunJSSE" );
      trustManagerFactory.init( trustStore );
      return Arrays.asList( trustManagerFactory.getTrustManagers( ) );
    }

    private static SSLSocketFactory buildDelegate( ) {
      final String provider = getProperty( PROP_USER_SSL_PROVIDER );
      final String protocol = getProperty( PROP_USER_SSL_PROTOCOL, DEFAULT_PROTOCOL );
      final String trustStorePath = getProperty( PROP_USER_SSL_TRUSTSTORE_PATH, DEFAULT_TRUSTSTORE_PATH );

      final SSLContext sslContext;
      try {
        sslContext = provider!=null ?
            SSLContext.getInstance( protocol, provider ) :
            SSLContext.getInstance( protocol );

        final List<TrustManager> trustManagers = Lists.newArrayList( );
        trustManagers.addAll( trustManagersForStore( ((AbstractKeyStore)SystemCredentials.getKeyStore( )).getKeyStore( ) ) );
        final File trustStore = new File( System.getProperty( "java.home" ), trustStorePath );
        if ( Objects.firstNonNull( USER_SSL_DEFAULT_CAS, false ) && trustStore.isFile( ) ) {
          trustManagers.addAll( trustManagersForStore( getTrustStore( trustStore ) ) );
        }

        sslContext.init(
            null,
            new TrustManager[]{ new DelegatingX509ExtendedTrustManager(
                Iterables.transform(
                    Iterables.filter(
                        trustManagers,
                        Predicates.instanceOf( javax.net.ssl.X509ExtendedTrustManager.class ) ),
                    CollectionUtils.cast( javax.net.ssl.X509ExtendedTrustManager.class ) ) ) },
            Crypto.getSecureRandomSupplier( ).get( ) );
        return sslContext.getSocketFactory( );
      } catch ( final GeneralSecurityException | IOException e ) {
        throw propagate(e);
      }
    }
  }
}
