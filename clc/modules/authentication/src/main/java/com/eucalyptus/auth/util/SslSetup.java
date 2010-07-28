package com.eucalyptus.auth.util;

import java.io.File;
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
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedKeyManager;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.system.SubDirectory;
import com.sun.net.ssl.internal.ssl.X509ExtendedTrustManager;

public class SslSetup {
  private static Logger       LOG            = Logger.getLogger( SslSetup.class );
  private static final String PROTOCOL       = "TLS";
  private static SSLContext   SERVER_CONTEXT = null;
  private static SSLContext   CLIENT_CONTEXT = null;
  
  static {
    SSLContext serverContext = null;
    SSLContext clientContext = null;
    System.setProperty( "javax.net.ssl.trustStore", SubDirectory.KEYS.toString( ) + File.separator + "euca.p12" );
    System.setProperty( "javax.net.ssl.keyStore", SubDirectory.KEYS.toString( ) + File.separator + "euca.p12" );
    System.setProperty( "javax.net.ssl.trustStoreType", "PKCS12" );
    System.setProperty( "javax.net.ssl.keyStoreType", "PKCS12" );
    System.setProperty( "javax.net.ssl.trustStorePassword", Component.eucalyptus.name( ) );
    System.setProperty( "javax.net.ssl.keyStorePassword", Component.eucalyptus.name( ) );
    System.setProperty( "javax.net.debug", "ssl" );//set this to "ssl" for debugging.
    try {
      serverContext = SSLContext.getInstance( "TLS" );
      serverContext.init( SslSetup.SimpleKeyManager.getKeyManagers( ), SslSetup.SimpleTrustManager.getTrustManagers( ), null );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new Error( "Failed to initialize the server-side SSLContext", e );
    }
    
    try {
      clientContext = SSLContext.getInstance( "TLS" );
      clientContext.init( SslSetup.SimpleKeyManager.getKeyManagers( ), SslSetup.SimpleTrustManager.getTrustManagers( ), null );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new Error( "Failed to initialize the client-side SSLContext", e );
    }
    
    SERVER_CONTEXT = serverContext;
    CLIENT_CONTEXT = clientContext;
  }
  
  public static SSLContext getServerContext( ) {
    return SERVER_CONTEXT;
  }

  public static SSLEngine getServerEngine() {
    SSLEngine engine = SslSetup.getServerContext( ).createSSLEngine( );
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
        return Component.eucalyptus.name( );
      }
      
      @Override
      public String chooseServerAlias( String arg0, Principal[] arg1, Socket arg2 ) {
        return Component.eucalyptus.name( );
      }
      
      @Override
      public X509Certificate[] getCertificateChain( String arg0 ) {
        if ( Component.eucalyptus.name( ).equals( arg0 ) ) {
          return trustedCerts;
        } else {
          return null;
        }
      }
      
      @Override
      public String[] getClientAliases( String arg0, Principal[] arg1 ) {
        return new String[] { Component.eucalyptus.name( ) };
      }
      
      @Override
      public PrivateKey getPrivateKey( String arg0 ) {
        if ( Component.eucalyptus.name( ).equals( arg0 ) ) {
          return trustedKey;
        } else {
          return null;
        }
      }
      
      @Override
      public String[] getServerAliases( String arg0, Principal[] arg1 ) {
        return new String[] { Component.eucalyptus.name( ) };
      }
      
      @Override
      public String chooseEngineClientAlias( String[] keyType, Principal[] issuers, SSLEngine engine ) {
        return Component.eucalyptus.name( );
      }
      
      @Override
      public String chooseEngineServerAlias( String keyType, Principal[] issuers, SSLEngine engine ) {
        return Component.eucalyptus.name( );
      }
      
    }
    
  }
  
  private static PrivateKey        trustedKey   = null;
  private static X509Certificate   trusted      = getTrustedCertificate( );
  private static X509Certificate[] trustedCerts = new X509Certificate[] { trusted };
  
  private static X509Certificate getTrustedCertificate( ) {
    try {
      synchronized ( SslSetup.class ) {
        if ( trusted == null ) {
          trusted = ( X509Certificate ) EucaKeyStore.getInstance( ).getCertificate( Component.eucalyptus.name( ) );
          trustedKey = EucaKeyStore.getInstance( ).getKeyPair( Component.eucalyptus.name( ), Component.eucalyptus.name( ) ).getPrivate( );
        }
        return trusted;
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new RuntimeException( e );
    }
  }
  
  public static class SimpleTrustManager extends TrustManagerFactorySpi {
    private static Logger             LOG       = Logger.getLogger( SslSetup.SimpleTrustManager.class );
    
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
  
}
