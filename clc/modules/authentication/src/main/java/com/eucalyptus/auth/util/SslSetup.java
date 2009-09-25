package com.eucalyptus.auth.util;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;

public class SslSetup {

  private static final String     PROTOCOL = "TLS";
  private static final SSLContext SERVER_CONTEXT = null;
  private static final SSLContext CLIENT_CONTEXT = null;

  static {
    SSLContext serverContext = null;
    SSLContext clientContext = null;
    try {
      KeyStore ks = EucaKeyStore.getInstance( ).getKeyStore( );
      KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
      kmf.init( ks, Component.eucalyptus.name( ).toCharArray( ) );
      serverContext = SSLContext.getInstance( "TLS" );
      serverContext.init( kmf.getKeyManagers( ), SslSetup.SimpleTrustManager.getTrustManagers( ), null );
    } catch ( Exception e ) {
      throw new Error( "Failed to initialize the server-side SSLContext", e );
    }

    try {
      clientContext = SSLContext.getInstance( "TLS" );
      clientContext.init( null, SslSetup.SimpleTrustManager.getTrustManagers( ), null );
    } catch ( Exception e ) {
      throw new Error( "Failed to initialize the client-side SSLContext", e );
    }

    SERVER_CONTEXT = serverContext;
    CLIENT_CONTEXT = clientContext;
  }

  public static SSLContext getServerContext( ) {
    return SERVER_CONTEXT;
  }

  public static SSLContext getClientContext( ) {
    return CLIENT_CONTEXT;
  }

  public static class SimpleTrustManager extends TrustManagerFactorySpi {
    private static Logger          LOG     = Logger.getLogger( SslSetup.SimpleTrustManager.class );
    private static X509Certificate trusted = null;

    private static X509Certificate getTrustedCertificate( ) {
      try {
        synchronized ( SslSetup.class ) {
          if ( trusted == null ) {
            KeyStore ks = EucaKeyStore.getInstance( ).getKeyStore( );
            trusted = ( X509Certificate ) ks.getCertificate( Component.eucalyptus.name( ) );
          }
          return trusted;
        }
      } catch ( KeyStoreException e ) {
        LOG.error( e, e );
        return null;
      }
    }

    private static final TrustManager singleton = new SimpleX509TrustManager( );

    public static TrustManager[] getTrustManagers( ) {
      return new TrustManager[] { singleton };
    }

    @Override
    protected TrustManager[] engineGetTrustManagers( ) {
      return getTrustManagers( );
    }

    @Override
    protected void engineInit( KeyStore keystore ) throws KeyStoreException {
    }

    @Override
    protected void engineInit( ManagerFactoryParameters managerFactoryParameters ) throws InvalidAlgorithmParameterException {
    }

    public static class SimpleX509TrustManager implements X509TrustManager {

      @Override
      public void checkClientTrusted( X509Certificate[] arg0, String arg1 ) throws CertificateException {
      }

      @Override
      public void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
      }

      @Override
      public X509Certificate[] getAcceptedIssuers( ) {
        return null;
      }
    }

  }

}
