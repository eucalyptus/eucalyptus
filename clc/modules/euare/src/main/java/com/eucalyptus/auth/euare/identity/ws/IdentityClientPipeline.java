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

import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.auth.euare.common.identity.Identity;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurationManager;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurations;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.SslUtils;
import com.eucalyptus.crypto.util.X509ExtendedTrustManagerSupport;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.Handlers;
import com.eucalyptus.ws.StackConfiguration;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 *
 */
@ComponentPart( Identity.class )
public class IdentityClientPipeline implements ChannelPipelineFactory {
  private static final String PROTOCOL = "TLS";
  private static final Supplier<Boolean> validateHostnames = new Supplier<Boolean>() {
    @Override
    public Boolean get() {
      return RegionConfigurations.isVerifyHostnames( );
    }
  };
  private static final Supplier<SSLContext> contextSupplier = Suppliers.memoizeWithExpiration( new Supplier<SSLContext>( ){
    @Override
    public SSLContext get() {
      try {
        final SSLContext clientContext = SSLContext.getInstance( PROTOCOL );
        clientContext.init(
            null,
            new TrustManager[ ]{ regionTrustManager },
            Crypto.getSecureRandomSupplier( ).get( )
        );
        return clientContext;
      } catch ( Exception e ) {
        throw new Error( "Failed to initialize the client-side SSLContext", e );
      }
    }
  }, 15, TimeUnit.MINUTES );
  private static final X509TrustManager regionTrustManager = new RegionTrustManager( validateHostnames );

  @Override
  public ChannelPipeline getPipeline( ) throws Exception {
    final ChannelPipeline pipeline = Channels.pipeline( );
    for ( Map.Entry<String, ChannelHandler> e : Handlers.channelMonitors( TimeUnit.SECONDS, StackConfiguration.CLIENT_INTERNAL_TIMEOUT_SECS ).entrySet( ) ) {
      pipeline.addLast( e.getKey( ), e.getValue( ) );
    }
    pipeline.addLast( "decoder", Handlers.newHttpResponseDecoder( ) );
    pipeline.addLast( "aggregator", Handlers.newHttpChunkAggregator() );
    pipeline.addLast( "encoder", Handlers.httpRequestEncoder() );
    pipeline.addLast( "serializer", Handlers.soapMarshalling() );
    pipeline.addLast( "wssec", Handlers.internalWsSecHandler() );
    pipeline.addLast( "addressing", Handlers.addressingHandler() );
    pipeline.addLast( "soap", Handlers.soapHandler() );
    pipeline.addLast( "binding", Handlers.bindingHandler( "www_eucalyptus_com_ns_identity_2015_03_01" ) );
    pipeline.addLast( "ssl-detection-handler", new Handlers.ClientSslHandler( "ssl-handler" ) {
      @Override
      protected SSLEngine createSSLEngine( final String peerHost, final int peerPort ) {
        return getSSLEngine( peerHost, peerPort );
      }
    } );
    pipeline.addLast( "remote", new RemotePathHandler( ) );
    return pipeline;
  }

  public static final class RemotePathHandler extends MessageStackHandler {
    @Override
    public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
      if ( event.getMessage( ) instanceof MappingHttpRequest ) {
        final MappingHttpRequest httpMessage = (MappingHttpRequest) event.getMessage();
        httpMessage.setServicePath( ComponentIds.lookup( Identity.class ).getServicePath() );
        String uri = URI.create( httpMessage.getUri( ) ).resolve( httpMessage.getServicePath( ) ).toString( );
        if ( RegionConfigurations.isUseSsl() ) {
          uri = uri.replace( "http://", "https://" );
        } else {
          uri = uri.replace( "https://", "http://" );
        }
        httpMessage.setUri( uri );
      }
    }
  }

  public static SSLContext getSSLContext( ) {
    return contextSupplier.get( );
  }

  public static SSLEngine getSSLEngine( final String peerHost, final int peerPort ) {
    try {
      final SSLParameters sslParams = new SSLParameters( );
      sslParams.setEndpointIdentificationAlgorithm( "HTTPS" );
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
    private final Supplier<Boolean> validateHostnames;

    public RegionTrustManager( final Supplier<Boolean> validateHostnames ) {
      this.validateHostnames = validateHostnames;
    }

    @Override
    public void checkServerTrusted( final X509Certificate[] x509Certificates, final String s, final SSLEngine sslEngine ) throws CertificateException {
      final X509Certificate serverCertificate = x509Certificates[0];
      final String hostname = sslEngine.getHandshakeSession( ).getPeerHost( );
      if ( Objects.firstNonNull( validateHostnames.get( ), Boolean.TRUE ) ) try {
        new BrowserCompatHostnameVerifier( ).verify( hostname, serverCertificate );
      } catch ( SSLException e ) {
        throw new CertificateException( "Server cert not valid for host" );
      }
      if ( !regionConfigurationManager.isRegionSSLCertificate( hostname, serverCertificate ) ) {
        throw new CertificateException( "Server cert not trusted" );
      }
    }
  }
}
