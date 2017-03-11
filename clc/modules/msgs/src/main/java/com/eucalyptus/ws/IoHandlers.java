/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.ws;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLEngine;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.handlers.IoAddressingHandler;
import com.eucalyptus.ws.handlers.IoBindingHandler;
import com.eucalyptus.ws.handlers.IoInternalHmacHandler;
import com.eucalyptus.ws.handlers.IoInternalWsSecHandler;
import com.eucalyptus.ws.handlers.IoMessageWrapperHandler;
import com.eucalyptus.ws.handlers.IoSoapMarshallingHandler;
import com.eucalyptus.ws.handlers.IoSoapHandler;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

/**
 *
 */
public class IoHandlers {

  public static final Map<String,ChannelHandler> extraMonitors = Maps.newConcurrentMap();

  private static final ChannelHandler soapHandler = new IoSoapHandler( );
  private static final ChannelHandler soapMarshallingHandler = new IoSoapMarshallingHandler( );
  private static final ChannelHandler internalWsSecHandler = new IoInternalWsSecHandler( );
  private static final ChannelHandler internalHmacHandler = new IoInternalHmacHandler( );
  private static final ChannelHandler addressingHandler = new IoAddressingHandler( );
  private static final ChannelHandler ioMessageHandler = new IoMessageWrapperHandler( );
  private static final ChannelHandler bindingHandler = new IoBindingHandler( IoBindingHandler.context( BindingManager.getDefaultBinding( ) ) );

  public static ChannelHandler httpRequestEncoder( ) {
    return new HttpRequestEncoder( );
  }

  public static ChannelHandler httpResponseDecoder( ) {
    return new HttpResponseDecoder( );
  }

  public static ChannelHandler newHttpChunkAggregator( ) {
    return new HttpObjectAggregator( StackConfiguration.CLIENT_HTTP_CHUNK_BUFFER_MAX );
  }

  public static ChannelHandler newQueryHttpChunkAggregator( ) {
    return new HttpObjectAggregator( StackConfiguration.PIPELINE_MAX_QUERY_REQUEST_SIZE );
  }

  public static ChannelHandler soapHandler( ) {
    return soapHandler;
  }

  public static ChannelHandler soapMarshalling( ) {
    return soapMarshallingHandler;
  }

  public static ChannelHandler internalWsSecHandler( ) {
    return internalWsSecHandler;
  }

  public static ChannelHandler getInternalHmacHandler( ) {
    return internalHmacHandler;
  }

  public static ChannelHandler addressingHandler( ) {
    return addressingHandler;
  }

  public static ChannelHandler addressingHandler( final String prefix ) {
    return new IoAddressingHandler( prefix );
  }

  public static ChannelHandler bindingHandler( ) {
    return bindingHandler;
  }

  public static ChannelHandler bindingHandler( final String bindingName ) {
    return bindingHandlers.getUnchecked( bindingName );
  }

  public static ChannelHandler ioMessageWrappingHandler( ) {
    return ioMessageHandler;
  }

  private static final LoadingCache<String, IoBindingHandler> bindingHandlers = CacheBuilder.newBuilder().build(
      new CacheLoader<String, IoBindingHandler>() {
        @Override
        public IoBindingHandler load( String bindingName ) {
          String maybeBindingName = "";
          if ( BindingManager.isRegisteredBinding( bindingName ) ) {
            return new IoBindingHandler( IoBindingHandler.context( BindingManager.getBinding( bindingName ) ) );
          } else if ( BindingManager.isRegisteredBinding( maybeBindingName = BindingManager.sanitizeNamespace( bindingName ) ) ) {
            return new IoBindingHandler( IoBindingHandler.context( BindingManager.getBinding( maybeBindingName ) ) );
          } else {
            throw Exceptions.trace( "Failed to find registerd binding for name: " + bindingName
                + ".  Also tried looking for sanitized name: "
                + maybeBindingName );
          }
        }
      });

  public static Map<String, ChannelHandler> channelMonitors( final TimeUnit unit, final long timeout ) {
    final Map<String, ChannelHandler> monitors = Maps.newHashMap( );
    monitors.put( "idlehandler", new IdleStateHandler( 0L, 0L, timeout, unit ) );
    monitors.put( "idlecloser", new ChannelInboundHandlerAdapter() {
      @Override
      public void userEventTriggered( final ChannelHandlerContext ctx, final Object evt ) throws Exception {
        if ( evt instanceof IdleStateEvent ) {
          IdleStateEvent e = (IdleStateEvent) evt;
          if ( e.state() == IdleState.ALL_IDLE ) {
            ctx.channel( ).close( );
          }
        }
      }
    } );
    monitors.putAll( extraMonitors );
    return monitors;
  }


  public abstract static class ClientSslHandler extends ChannelOutboundHandlerAdapter {
    private final String sslHandlerName;

    public ClientSslHandler( final String sslHandlerName ) {
      this.sslHandlerName = sslHandlerName;
    }

    protected abstract SSLEngine createSSLEngine( final String peerHost, final int peerPort );

    @Override
    public void write( final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise ) throws Exception {
      if ( msg instanceof MappingHttpRequest ) {
        ctx.pipeline( ).remove( this );
        if ( isHttps( (MappingHttpRequest)msg ) ) {
          final URI uri = URI.create( ( (MappingHttpRequest) msg ).getUri( ) );
          final SslHandler sslHandler =
              new SslHandler( createSSLEngine( uri.getHost( ), uri.getPort( )==-1?443:uri.getPort( ) ) );
          ctx.pipeline( ).addFirst( sslHandlerName, sslHandler );
        }
      }
      super.write( ctx, msg, promise );
    }

    private boolean isHttps( final MappingHttpRequest o ) {
      return !o.getUri( ).startsWith( "http://" );
    }
  }
}
