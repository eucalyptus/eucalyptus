/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 ************************************************************************/
package com.eucalyptus.auth.euare.identity.ws;

import static java.lang.System.getProperty;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import com.eucalyptus.auth.euare.common.identity.Identity;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurationManager;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurations;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.DelegatingX509ExtendedTrustManager;
import com.eucalyptus.crypto.util.SslUtils;
import com.eucalyptus.crypto.util.X509ExtendedTrustManagerSupport;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.IO;
import com.eucalyptus.util.Pair;
import com.eucalyptus.ws.Handlers;
import com.eucalyptus.ws.IoHandlers;
import com.eucalyptus.ws.StackConfiguration;
import com.eucalyptus.ws.client.MonitoredSocketChannelInitializer;
import com.eucalyptus.ws.handlers.IoMessageWrapperHandler;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.SocketChannel;

/**
 *
 */
@ComponentPart( Identity.class )
public class IdentityClientChannelInitializer extends MonitoredSocketChannelInitializer {
  private static final String PROTOCOL = "TLS";
  private static final String PROP_SSL_TRUSTSTORE_PASSWORD = "com.eucalyptus.auth.euare.identity.regionSslTrustStorePassword";
  private static final String PROP_SSL_TRUSTSTORE_TYPE = "com.eucalyptus.auth.euare.identity.regionSslTrustStoreType";
  private static final String PROP_SSL_TRUSTSTORE_PATH = "com.eucalyptus.auth.euare.identity.regionSslTrustStorePath";
  private static final String DEFAULT_TRUSTSTORE_PASSWORD = "changeit";
  private static final String DEFAULT_TRUSTSTORE_PATH = "lib/security/cacerts";

  private static final AtomicReference<Pair<Long,KeyStore>> sslTrustStore = new AtomicReference<>( );
  private static final Supplier<Boolean> useDefaultCAs = new Supplier<Boolean>() {
    @Override
    public Boolean get() {
      return RegionConfigurations.isUseDefaultCAs();
    }
  };
  private static final Supplier<SSLContext> contextSupplier = Suppliers.memoizeWithExpiration( new Supplier<SSLContext>( ){
    @Override
    public SSLContext get() {
      final String trustStorePath = getProperty( PROP_SSL_TRUSTSTORE_PATH, DEFAULT_TRUSTSTORE_PATH );
      try {
        final List<TrustManager> trustManagers = Lists.newArrayList();
        trustManagers.add( regionTrustManager );
        final File trustStore = new File( System.getProperty( "java.home" ), trustStorePath );
        if ( trustStore.isFile( ) ) {
          trustManagers.add( buildDelegatingTrustManager(
              trustManagersForStore( getTrustStore( trustStore ) ),
              useDefaultCAs ) );
        }

        final SSLContext clientContext = SSLContext.getInstance( PROTOCOL );
        clientContext.init(
            null,
            new TrustManager[]{ buildDelegatingTrustManager( trustManagers, Suppliers.ofInstance( true ) ) },
            Crypto.getSecureRandomSupplier( ).get( )
        );
        return clientContext;
      } catch ( Exception e ) {
        throw new Error( "Failed to initialize the client-side SSLContext", e );
      }
    }
  }, 15, TimeUnit.MINUTES );
  private static final X509TrustManager regionTrustManager = new RegionTrustManager( );

  private static TrustManager buildDelegatingTrustManager(
      final List<TrustManager> trustManagers,
      final Supplier<Boolean> enable
  ) {
    return new DelegatingX509ExtendedTrustManager(
        Iterables.transform(
            Iterables.filter(
                trustManagers,
                Predicates.instanceOf( javax.net.ssl.X509ExtendedTrustManager.class ) ),
            CollectionUtils.cast( javax.net.ssl.X509ExtendedTrustManager.class ) ),
        enable
    );
  }

  @Override
  protected void initChannel( final SocketChannel socketChannel ) throws Exception {
    super.initChannel( socketChannel );
    final ChannelPipeline pipeline = socketChannel.pipeline( );
    pipeline.addLast( "decoder", IoHandlers.httpResponseDecoder( ) );
    pipeline.addLast( "aggregator", IoHandlers.newHttpChunkAggregator() );
    pipeline.addLast( "encoder", IoHandlers.httpRequestEncoder() );
    pipeline.addLast( "wrapper", IoHandlers.ioMessageWrappingHandler( ) );
    pipeline.addLast( "serializer", IoHandlers.soapMarshalling() );
    pipeline.addLast( "wssec", IoHandlers.internalWsSecHandler() );
    pipeline.addLast( "addressing", IoHandlers.addressingHandler() );
    pipeline.addLast( "soap", IoHandlers.soapHandler() );
    pipeline.addLast( "binding", IoHandlers.bindingHandler( "www_eucalyptus_com_ns_identity_2016_10_01" ) );
    pipeline.addLast( "ssl-detection-handler", new IoHandlers.ClientSslHandler( "ssl-handler" ) {
      @Override
      protected SSLEngine createSSLEngine( final String peerHost, final int peerPort ) {
        return getSSLEngine( peerHost, peerPort );
      }
    } );
    pipeline.addLast( "remote", new RemotePathHandler( ) );
  }

  public static final class RemotePathHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write( final io.netty.channel.ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise ) throws Exception {
      if ( msg instanceof MappingHttpRequest ) {
        final MappingHttpRequest httpMessage = (MappingHttpRequest) msg;
        httpMessage.setServicePath( ComponentIds.lookup( Identity.class ).getServicePath() );
        String uri = URI.create( httpMessage.getUri( ) ).resolve( httpMessage.getServicePath( ) ).toString( );
        if ( RegionConfigurations.isUseSsl() ) {
          uri = uri.replace( "http://", "https://" );
        } else {
          uri = uri.replace( "https://", "http://" );
        }
        httpMessage.setUri( uri );
      }
      super.write( ctx, msg, promise );
    }
  }

  public static SSLContext getSSLContext( ) {
    return contextSupplier.get( );
  }

  public static SSLEngine getSSLEngine( final String peerHost, final int peerPort ) {
    try {
      final SSLParameters sslParams = new SSLParameters( );
      if ( RegionConfigurations.isVerifyHostnames( ) ) {
        sslParams.setEndpointIdentificationAlgorithm( "HTTPS" );
      }
      final SSLContext clientContext = getSSLContext( );
      final SSLEngine engine = clientContext.createSSLEngine( peerHost, peerPort );
      engine.setSSLParameters( sslParams );
      engine.setUseClientMode( true );
      engine.setEnabledProtocols( SslUtils.getEnabledProtocols(
          RegionConfigurations.getSslProtocols( ),
          engine.getSupportedProtocols( ) ) );
      engine.setEnabledCipherSuites( SslUtils.getEnabledCipherSuites(
          RegionConfigurations.getSslCiphers( ),
          engine.getSupportedCipherSuites() ) );
      return engine;
    } catch ( Exception e ) {
      throw new Error( "Failed to initialize the client-side SSLContext", e );
    }
  }

  public static final class RegionTrustManager extends X509ExtendedTrustManagerSupport {
    private final RegionConfigurationManager regionConfigurationManager = new RegionConfigurationManager( );

    @Override
    public void checkServerTrusted( final X509Certificate[] x509Certificates, final String s, final SSLEngine sslEngine ) throws CertificateException {
      final X509Certificate serverCertificate = x509Certificates[0];
      final String hostname = sslEngine.getHandshakeSession( ).getPeerHost( );
      final SSLParameters sslParameters = sslEngine.getSSLParameters( );
      if ( sslParameters != null && "HTTPS".equals( sslParameters.getEndpointIdentificationAlgorithm( ) ) ) try {
        new BrowserCompatHostnameVerifier( ).verify( hostname, serverCertificate );
      } catch ( SSLException e ) {
        throw new CertificateException( "Server cert not valid for host" );
      }
      if ( !regionConfigurationManager.isRegionSSLCertificate( hostname, serverCertificate ) ) {
        throw new CertificateException( "Server cert not trusted" );
      }
    }
  }

  private static KeyStore getTrustStore( final File trustStore ) throws GeneralSecurityException, IOException {
    final Pair<Long,KeyStore> currentTrustStore = sslTrustStore.get( );
    InputStream trustStoreIn = null;
    if ( currentTrustStore != null && currentTrustStore.getLeft( ) == trustStore.lastModified( ) ) {
      return currentTrustStore.getRight( );
    } else try {
      final String trustStoreType = getProperty( PROP_SSL_TRUSTSTORE_TYPE, KeyStore.getDefaultType( ) );
      final String trustStorePassword = getProperty( PROP_SSL_TRUSTSTORE_PASSWORD, DEFAULT_TRUSTSTORE_PASSWORD );
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
    return Arrays.asList( trustManagerFactory.getTrustManagers() );
  }
}
