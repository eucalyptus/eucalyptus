/* Copyright (c) 2001-2008, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hsqldb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedKeyManager;
import org.apache.log4j.Logger;
import com.sun.net.ssl.internal.ssl.X509ExtendedTrustManager;

/**
 * We need to override the way that hsqldb gets its secure sockets to use our internally mananged credentials.
 */
public final class HsqlSocketFactorySecure extends HsqlSocketFactory {
  private static Logger LOG = Logger.getLogger( HsqlSocketFactorySecure.class );
  protected SSLSocketFactory       socketFactory;
  protected SSLServerSocketFactory serverSocketFactory;
  private static final String PROTOCOL       = "TLS";
  private static SSLContext   SERVER_CONTEXT = null;
  private static SSLContext   CLIENT_CONTEXT = null;
  private static String KEYS = System.getProperty("euca.var.dir") + File.separator + "keys";
  private static String KEYSTORE = KEYS + File.separator + "euca.p12";
  private static String EUCALYPTUS = "eucalyptus";
  private static PrivateKey pk = null; 
  private static X509Certificate cert = null;
  static {
    SSLContext serverContext = null;
    SSLContext clientContext = null;
    System.setProperty( "javax.net.ssl.trustStore", KEYSTORE );
    System.setProperty( "javax.net.ssl.keyStore", KEYSTORE );
    System.setProperty( "javax.net.ssl.trustStoreType", "PKCS12" );
    System.setProperty( "javax.net.ssl.keyStoreType", "PKCS12" );
    System.setProperty( "javax.net.ssl.trustStorePassword", EUCALYPTUS );
    System.setProperty( "javax.net.ssl.keyStorePassword", EUCALYPTUS );
    System.setProperty( "javax.net.debug", "none" );//set this to "ssl" for debugging.
    try {
      serverContext = SSLContext.getInstance( "TLS" );
      serverContext.init( HsqlSocketFactorySecure.SimpleKeyManager.getKeyManagers( ), HsqlSocketFactorySecure.SimpleTrustManager.getTrustManagers( ), null );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new Error( "Failed to initialize the server-side SSLContext", e );
    }
    
    try {
      clientContext = SSLContext.getInstance( "TLS" );
      clientContext.init( HsqlSocketFactorySecure.SimpleKeyManager.getKeyManagers( ), HsqlSocketFactorySecure.SimpleTrustManager.getTrustManagers( ), null );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new Error( "Failed to initialize the client-side SSLContext", e );
    }
    loadKeystore( );
    SERVER_CONTEXT = serverContext;
    CLIENT_CONTEXT = clientContext;
  }

  private static void loadKeystore( ) {
    FileInputStream fin = null;      
    try {
      KeyStore keyStore = KeyStore.getInstance( "pkcs12", "BC" );
      if ( ( new File( KEYSTORE ) ).exists( ) ) {
        fin = new FileInputStream( KEYSTORE );
        keyStore.load( fin, EUCALYPTUS.toCharArray( ) );
        pk = ( PrivateKey ) keyStore.getKey( EUCALYPTUS, EUCALYPTUS.toCharArray( ) );
        cert = ( X509Certificate ) keyStore.getCertificate( EUCALYPTUS );
      } 
    } catch ( Throwable t ) {
      LOG.error( t, t );
    } finally {
      if ( fin != null ) {
        try {
    	  fin.close();
    	} catch ( IOException e ) {
    	  LOG.error ( e );
        }
      }
    }
  }
  
  public static SSLContext getServerContext( ) {
    return SERVER_CONTEXT;
  }

  public static SSLEngine getServerEngine() {
    SSLEngine engine = HsqlSocketFactorySecure.getServerContext( ).createSSLEngine( );
    engine.setUseClientMode( false );
    return engine;
  }
  public static SSLContext getClientContext( ) {
    return CLIENT_CONTEXT;
  }
  
  public static class SimpleKeyManager extends KeyManagerFactorySpi {
    private static KeyManager singleton = new SimplePKCS12KeyManager( );
    
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
    
    public static class SimplePKCS12KeyManager extends X509ExtendedKeyManager {
      
      @Override
      public String chooseClientAlias( String[] arg0, Principal[] arg1, Socket arg2 ) {
        return EUCALYPTUS;
      }
      
      @Override
      public String chooseServerAlias( String arg0, Principal[] arg1, Socket arg2 ) {
        return EUCALYPTUS;
      }
      
      @Override
      public X509Certificate[] getCertificateChain( String arg0 ) {
        if ( EUCALYPTUS.equals( arg0 ) ) {
          return trustedCerts;
        } else {
          return null;
        }
      }
      
      @Override
      public String[] getClientAliases( String arg0, Principal[] arg1 ) {
        return new String[] { EUCALYPTUS };
      }
      
      @Override
      public PrivateKey getPrivateKey( String arg0 ) {
        if ( EUCALYPTUS.equals( arg0 ) ) {
          return trustedKey;
        } else {
          return null;
        }
      }
      
      @Override
      public String[] getServerAliases( String arg0, Principal[] arg1 ) {
        return new String[] { EUCALYPTUS };
      }
      
      @Override
      public String chooseEngineClientAlias( String[] keyType, Principal[] issuers, SSLEngine engine ) {
        return EUCALYPTUS;
      }
      
      @Override
      public String chooseEngineServerAlias( String keyType, Principal[] issuers, SSLEngine engine ) {
        return EUCALYPTUS;
      }
      
    }
    
  }
  
  private static PrivateKey        trustedKey   = null;
  private static X509Certificate   trusted      = getTrustedCertificate( );
  private static X509Certificate[] trustedCerts = new X509Certificate[] { trusted };
  
  private static X509Certificate getTrustedCertificate( ) {
    try {
      synchronized ( HsqlSocketFactorySecure.class ) {
        if ( trusted == null ) {
          trusted = cert;
          trustedKey = pk;
        }
        return trusted;
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new RuntimeException( e );
    }
  }
  
  public static class SimpleTrustManager extends TrustManagerFactorySpi {
    private static Logger             LOG       = Logger.getLogger( HsqlSocketFactorySecure.SimpleTrustManager.class );
    
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
  
  protected HsqlSocketFactorySecure( ) throws Exception {}
  
  public void configureSocket( Socket socket ) {
    super.configureSocket( socket );
  }
  
  public ServerSocket createServerSocket( int port ) throws Exception {
    return ( SSLServerSocket ) getServerSocketFactoryImpl( ).createServerSocket( port );
  }
  
  public ServerSocket createServerSocket( int port, String address ) throws Exception {
    InetAddress addr = InetAddress.getByName( address );
    return ( SSLServerSocket ) getServerSocketFactoryImpl( ).createServerSocket( port, 128, addr );
  }
  
  public Socket createSocket( String host, int port ) throws Exception {
    SSLSocket socket = ( SSLSocket ) getSocketFactoryImpl( ).createSocket( host, port );
    socket.startHandshake( );
    return socket;
  }
  
  public boolean isSecure( ) {
    return true;
  }
  
  protected SSLServerSocketFactory getServerSocketFactoryImpl( ) throws Exception {
    synchronized ( HsqlSocketFactorySecure.class ) {
      if ( serverSocketFactory == null ) {
        serverSocketFactory =  HsqlSocketFactorySecure.getServerContext( ).getServerSocketFactory( );
      }
    }
    return ( SSLServerSocketFactory ) serverSocketFactory;
  }
  
  protected SSLSocketFactory getSocketFactoryImpl( ) throws Exception {
    synchronized ( HsqlSocketFactorySecure.class ) {
      if ( socketFactory == null ) {
        socketFactory =  HsqlSocketFactorySecure.getClientContext( ).getSocketFactory( );
      }
    }
    return ( SSLSocketFactory ) socketFactory;
  }
  
}
