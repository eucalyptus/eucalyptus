package com.eucalyptus.crypto.util;

import static java.util.Collections.singleton;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;
import static com.eucalyptus.crypto.util.SslSetup.SslCipherBuilder.ciphers;
import static com.eucalyptus.crypto.util.SslSetup.SslCipherSuiteBuilderParams.params;
import static com.google.common.base.CharMatcher.anyOf;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.contains;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import java.io.File;
import java.net.Socket;
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
import java.util.Map;
import java.util.Set;
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
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.system.SubDirectory;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapMaker;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;
import com.sun.net.ssl.internal.ssl.X509ExtendedTrustManager;

@ConfigurableClass( root = "bootstrap.webservices.ssl",
                    description = "Parameters controlling the SSL configuration for the web services endpoint." )
public class SslSetup {
  private static final Logger LOG             = Logger.getLogger( SslSetup.class );
  private static final String PROTOCOL        = "TLS";
  private static SSLContext   SERVER_CONTEXT  = null;
  private static SSLContext   CLIENT_CONTEXT  = null;
  private static final LoadingCache<SslCipherSuiteBuilderParams,String[]> SSL_CIPHER_LOOKUP =
	  CacheBuilder.newBuilder().maximumSize(32).build(CacheLoader.from(SslCipherSuiteBuilder.INSTANCE) );
  @ConfigurableField( description = "Alias of the certificate entry in euca.p12 to use for SSL for webservices.",
                      changeListener = SslCertChangeListener.class )
  public static String        SERVER_ALIAS    = ComponentIds.lookup( Eucalyptus.class ).name( );
  @ConfigurableField( description = "Password of the private key corresponding to the specified certificate for SSL for webservices.",
                      changeListener = SslPasswordChangeListener.class )
  public static String        SERVER_PASSWORD = ComponentIds.lookup( Eucalyptus.class ).name( );
  @ConfigurableField( description = "SSL ciphers for webservices." )
  public static String        SERVER_SSL_CIPHERS = "RSA:DSS:ECDSA:+RC4:+3DES:TLS_EMPTY_RENEGOTIATION_INFO_SCSV:!NULL:!EXPORT:!EXPORT1024:!MD5:!DES";

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
    engine.setEnabledCipherSuites( getEnabledCipherSuites( SERVER_SSL_CIPHERS, engine.getSupportedCipherSuites() ) );
    return engine;
  }
  
  public static SSLContext getClientContext( ) {
    return CLIENT_CONTEXT;
  }

  public static String[] getEnabledCipherSuites( final String cipherStrings, final String[] supportedCipherSuites ) {
    return SSL_CIPHER_LOOKUP.getUnchecked( params(cipherStrings, supportedCipherSuites) );
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

  static final class SslCipherSuiteBuilderParams {
    private final String cipherStrings;
    private final String[] supportedCipherSuites;

    private SslCipherSuiteBuilderParams( final String cipherStrings,
                                         final String[] supportedCipherSuites ) {
      this.cipherStrings = cipherStrings;
      this.supportedCipherSuites = supportedCipherSuites.clone();
    }

    public String getCipherStrings() {
      return cipherStrings;
    }

    public String[] getSupportedCipherSuites() {
      return supportedCipherSuites;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final SslCipherSuiteBuilderParams that = (SslCipherSuiteBuilderParams) o;

      if (!cipherStrings.equals(that.cipherStrings)) return false;
      if (!Arrays.equals(supportedCipherSuites, that.supportedCipherSuites)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = cipherStrings.hashCode();
      result = 31 * result + Arrays.hashCode(supportedCipherSuites);
      return result;
    }

    static SslCipherSuiteBuilderParams params( final String cipherStrings,
                                               final String[] supportedCipherSuites  ) {
      return new SslCipherSuiteBuilderParams( cipherStrings, supportedCipherSuites );
    }
  }

  private enum SslCipherSuiteBuilder implements Function<SslCipherSuiteBuilderParams,String[]>{
    INSTANCE;

    @Override
    public String[] apply( final SslCipherSuiteBuilderParams params ) {
      return ciphers()
          .with( params.getCipherStrings() )
          .enabledCipherSuites( params.getSupportedCipherSuites() );
    }
  }

  /**
   * Cipher suite builder that allows the OpenSSL syntax for cipher
   * exclusions (! prefix) and supports the ALL, NULL, and EXPORT lists.
   *
   * This also supports + to combine algorithms (e.g. "RSA+AES") and to
   * move ciphers to the end of the list (e.g. "+RC4")
   */
  static final class SslCipherBuilder {
    private final Set<String> cipherStringsSteps = Sets.newLinkedHashSet();
    private final Set<String> excludedCipherStrings = Sets.newHashSet();

    static SslCipherBuilder ciphers() {
      return new SslCipherBuilder();
    }

    SslCipherBuilder with( final String cipherStrings ) {
      return with( Splitter.on(anyOf(": ,") ).omitEmptyStrings().trimResults().split( cipherStrings ) );
    }

    SslCipherBuilder with( final Iterable<String> cipherStrings ) {
      addAll(cipherStringsSteps, filter(cipherStrings, not(CipherStringPrefixes.NOT)));
      addAll(excludedCipherStrings, transform(filter(cipherStrings, CipherStringPrefixes.NOT), CipherStringPrefixes.NOT.cleaner()));
      return this;
    }

    String[] enabledCipherSuites( final String[] supportedCipherSuiteArray ) {
      final ImmutableList<String> supportedCipherSuites = copyOf(supportedCipherSuiteArray);
      final ImmutableList<String> excludedCipherSuites = explodeCipherStrings(excludedCipherStrings, supportedCipherSuites);
      final List<String> cipherSuites = newArrayList();
      for ( final String cipherString : cipherStringsSteps ) {
        if ( CipherStringPrefixes.PLUS.apply(cipherString) ) {
          final String cipherStringToShift = CipherStringPrefixes.PLUS.cleaner().apply(cipherString);
          shift(cipherSuites, explodeCipherStrings(singleton(cipherStringToShift), supportedCipherSuites));
        } else {
          cipherSuites.addAll(explodeCipherStrings(singleton(cipherString), supportedCipherSuites));
        }
      }
      return toArray(filter(cipherSuites, and(in(supportedCipherSuites), not(in(excludedCipherSuites)))), String.class);
    }

    void shift( final List<String> cipherSuites,
                final List<String> ciphersSuitesToShift ) {
      // Shift ciphers to the end of the list
      for ( final String cipherSuite : ciphersSuitesToShift ) {
        if ( cipherSuites.remove( cipherSuite ) ) {
          cipherSuites.add( cipherSuite );
        }
      }
    }


    private ImmutableList<String> explodeCipherStrings( final Set<String> cipherStrings,
                                                        final ImmutableList<String> supportedCipherSuites) {
      return copyOf(concat(transform(cipherStrings, cipherStringExploder(supportedCipherSuites))));
    }

    private Function<String,Iterable<String>> cipherStringExploder( final ImmutableList<String> supportedCipherSuites ) {
      return new Function<String,Iterable<String>>() {
        @Override
        public Iterable<String> apply( final String cipherString ) {
          if ( "ALL".equals( cipherString ) ) {
            return supportedCipherSuites;
          } else if ( cipherString.startsWith("TLS_") || cipherString.startsWith("SSL_") ) {
            return singleton(cipherString);
          } else {
            return filter( supportedCipherSuites, toPredicate(cipherString));
          }
        }
      };
    }

    private Predicate<CharSequence> toPredicate( final String cipherString ) {
      final List<Predicate<CharSequence>> predicates = newArrayList();
      for ( final String cipherStringPart : Splitter.on("+").split(cipherString) ) {
        predicates.add( contains(compile("_" + quote(cipherStringPart)  + "(_|$)")) );
      }
      return and(predicates);
    }

    private enum CipherStringPrefixes implements Predicate<String> {
      NOT("!"),
      PLUS("+");

      private final String prefix;

      private CipherStringPrefixes( final String prefix ) {
        this.prefix = prefix;
      }

      @Override
      public boolean apply( final String value ) {
        return value.startsWith( prefix );
      }

      public Function<String,String> cleaner() {
        return new Function<String,String>(){
          @Override
          public String apply( final String value ) {
            return value.substring(1);
          }
        };
      }
    }
  }
  
}
