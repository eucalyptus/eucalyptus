package com.eucalyptus.crypto.util;

import java.io.File;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedKeyManager;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.system.SubDirectory;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ObjectArrays;
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
    System.setProperty( "javax.net.ssl.trustStorePassword", ComponentIds.lookup( Eucalyptus.class ).name( ) );
    System.setProperty( "javax.net.ssl.keyStorePassword", ComponentIds.lookup( Eucalyptus.class ).name( ) );
//    System.setProperty( "javax.net.debug", "ssl" );//set this to "ssl" for debugging.
    try {
      serverContext = SSLContext.getInstance( "TLS" );
      serverContext.init( SslSetup.ServerKeyManager.getKeyManagers( ), SslSetup.ServerTrustManager.getTrustManagers( ), null );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new Error( "Failed to initialize the server-side SSLContext", e );
    }
    
    try {
      clientContext = SSLContext.getInstance( "TLS" );
      clientContext.init( SslSetup.ClientKeyManager.getKeyManagers( ), SslSetup.ClientTrustManager.getTrustManagers( ), null );
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
  
  public static SSLEngine getServerEngine( ) {//TODO:GRZE: @Configurability
    SSLEngine engine = SslSetup.getServerContext( ).createSSLEngine( );
    engine.setUseClientMode( false );
    return engine;
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
        return SslSetup.SERVER_CERT_ALIAS;
      }
      
      @Override
      public String chooseServerAlias( String arg0, Principal[] arg1, Socket arg2 ) {
        return SslSetup.SERVER_CERT_ALIAS;
      }
      
      @Override
      public X509Certificate[] getCertificateChain( String arg0 ) {
        if ( SslSetup.SERVER_CERT_ALIAS.equals( arg0 ) ) {
          return memoizedServerCertSupplier.get( );
        } else {
          return null;
        }
      }
      
      @Override
      public String[] getClientAliases( String arg0, Principal[] arg1 ) {
        return new String[] { SslSetup.SERVER_CERT_ALIAS };
      }
      
      @Override
      public PrivateKey getPrivateKey( String arg0 ) {
        if ( SslSetup.SERVER_CERT_ALIAS.equals( arg0 ) ) {
          return serverPrivateKeySupplier.get( );
        } else {
          return null;
        }
      }
      
      @Override
      public String[] getServerAliases( String arg0, Principal[] arg1 ) {
        return new String[] { SslSetup.SERVER_CERT_ALIAS };
      }
      
      @Override
      public String chooseEngineClientAlias( String[] keyType, Principal[] issuers, SSLEngine engine ) {
        return SslSetup.SERVER_CERT_ALIAS;
      }
      
      @Override
      public String chooseEngineServerAlias( String keyType, Principal[] issuers, SSLEngine engine ) {
        return SslSetup.SERVER_CERT_ALIAS;
      }
      
    }
    
  }
  
  public static String                             SERVER_CERT_ALIAS          = ComponentIds.lookup( Eucalyptus.class ).name( );
  public static String                             SERVER_CERT_PASSWORD       = ComponentIds.lookup( Eucalyptus.class ).name( );
  private static final Supplier<PrivateKey>        serverPrivateKeySupplier   = new Supplier<PrivateKey>( ) {
                                                                                
                                                                                @Override
                                                                                public PrivateKey get( ) {
                                                                                  try {
                                                                                    return SystemCredentials.getKeyStore( ).getKeyPair(
                                                                                      SslSetup.SERVER_CERT_ALIAS,
                                                                                      SslSetup.SERVER_CERT_ALIAS ).getPrivate( );
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
                                                                                      SslSetup.SERVER_CERT_ALIAS );
                                                                                    return certs;
                                                                                  } catch ( GeneralSecurityException ex ) {
                                                                                    LOG.error( ex, ex );
                                                                                    return certs;
                                                                                  }
                                                                                }
                                                                              };
  private static Supplier<PrivateKey>              memoizedPrivateKeySupplier = Suppliers.memoizeWithExpiration( serverPrivateKeySupplier, 1l, TimeUnit.MINUTES );
  private static Supplier<X509Certificate[]>       memoizedServerCertSupplier = Suppliers.memoizeWithExpiration( serverCertSupplier, 1l, TimeUnit.MINUTES );
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
  
}
