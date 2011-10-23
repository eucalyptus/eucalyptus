/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.ws;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelSink;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMessageEncoder;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.handler.timeout.WriteTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentMessages;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.SslSetup;
import com.eucalyptus.empyrean.ServiceTransitionType;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Timers;
import com.eucalyptus.ws.handlers.BindingHandler;
import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.eucalyptus.ws.handlers.http.HttpUtils;
import com.eucalyptus.ws.handlers.http.NioHttpDecoder;
import com.eucalyptus.ws.protocol.AddressingHandler;
import com.eucalyptus.ws.protocol.SoapHandler;
import com.eucalyptus.ws.server.NioServerHandler;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Handlers {
  private static Logger                                      LOG                    = Logger.getLogger( Handlers.class );
  private static final ChannelHandler                        soapMarshallingHandler = new SoapMarshallingHandler( );
  private static final ChannelHandler                        httpRequestEncoder     = new NioHttpRequestEncoder( );
  private static final ChannelHandler                        soapHandler            = new SoapHandler( );
  private static final ChannelHandler                        addressingHandler      = new AddressingHandler( );
  private static final ConcurrentMap<String, ChannelHandler> bindingHandlers        = new ConcurrentHashMap<String, ChannelHandler>( );
  private static final HashedWheelTimer                      timer                  = new HashedWheelTimer( );                         //TODO:GRZE: configurable
  private static Boolean                                     STAGE_HANDLER_LOGGING  = Boolean.TRUE;                                    //GRZE: @Configurable
                                                                                                                                        
  private static class LoggingHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler {
    private final ChannelUpstreamHandler   upstream;
    private final ChannelDownstreamHandler downstream;
    private final ChannelHandler           handler;
    
    private LoggingHandler( final ChannelHandler handler ) {
      super( );
      this.handler = handler;
      this.upstream = ( ChannelUpstreamHandler ) ( ( handler instanceof ChannelUpstreamHandler ) ? handler : null );
      this.downstream = ( ChannelDownstreamHandler ) ( ( handler instanceof ChannelDownstreamHandler ) ? handler : null );
    }
    
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Override
    public void handleDownstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
      if ( e instanceof MessageEvent ) {
        Timers.loggingWrapper( new Callable( ) {
          
          @Override
          public Object call( ) throws Exception {
            if ( LoggingHandler.this.downstream != null ) {
              LoggingHandler.this.downstream.handleDownstream( ctx, e );
            }
            return e;
          }
          
        } ).call( );
      } else {
        if ( LoggingHandler.this.downstream != null ) {
          LoggingHandler.this.downstream.handleDownstream( ctx, e );
        } else {
          ctx.sendDownstream( e );
        }
      }
    }
    
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Override
    public void handleUpstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
      if ( e instanceof MessageEvent ) {
        Timers.loggingWrapper( new Callable( ) {
          
          @Override
          public Object call( ) throws Exception {
            if ( LoggingHandler.this.upstream != null ) {
              LoggingHandler.this.upstream.handleUpstream( ctx, e );
            }
            return e;
          }
          
        } ).call( );
      } else {
        if ( LoggingHandler.this.upstream != null ) {
          LoggingHandler.this.upstream.handleUpstream( ctx, e );
        } else {
          ctx.sendUpstream( e );
        }
      }
      
    }
    
    @Override
    public int hashCode( ) {
      return this.handler.hashCode( );
    }
    
    @Override
    public boolean equals( final Object obj ) {
      return this.handler.equals( obj );
    }
    
    @Override
    public String toString( ) {
      return this.handler.toString( );
    }
  }
  
  public static ChannelHandler wrapHandler( final ChannelHandler handler ) {
    return STAGE_HANDLER_LOGGING ? new LoggingHandler( handler ) : handler;
  }
  
  enum ServerPipelineFactory implements ChannelPipelineFactory {
    INSTANCE;
    @Override
    public ChannelPipeline getPipeline( ) throws Exception {
      final ChannelPipeline pipeline = new ChannelPipeline( ) {
        private final ChannelPipeline delegate = Channels.pipeline( );
        
        @Override
        public void addFirst( final String name, final ChannelHandler handler ) {
          this.delegate.addFirst( name, Handlers.wrapHandler( handler ) );
        }
        
        @Override
        public void addLast( final String name, final ChannelHandler handler ) {
          this.delegate.addLast( name, Handlers.wrapHandler( handler ) );
        }
        
        @Override
        public void addBefore( final String baseName, final String name, final ChannelHandler handler ) {
          this.delegate.addBefore( baseName, name, Handlers.wrapHandler( handler ) );
        }
        
        @Override
        public void addAfter( final String baseName, final String name, final ChannelHandler handler ) {
          this.delegate.addAfter( baseName, name, Handlers.wrapHandler( handler ) );
        }
        
        @Override
        public void remove( final ChannelHandler handler ) {
          this.delegate.remove( handler );
        }
        
        @Override
        public ChannelHandler remove( final String name ) {
          return this.delegate.remove( name );
        }
        
        @Override
        public <T extends ChannelHandler> T remove( final Class<T> handlerType ) {
          return this.delegate.remove( handlerType );
        }
        
        @Override
        public ChannelHandler removeFirst( ) {
          return this.delegate.removeFirst( );
        }
        
        @Override
        public ChannelHandler removeLast( ) {
          return this.delegate.removeLast( );
        }
        
        @Override
        public void replace( final ChannelHandler oldHandler, final String newName, final ChannelHandler newHandler ) {
          this.delegate.replace( oldHandler, newName, newHandler );
        }
        
        @Override
        public ChannelHandler replace( final String oldName, final String newName, final ChannelHandler newHandler ) {
          return this.delegate.replace( oldName, newName, newHandler );
        }
        
        @Override
        public <T extends ChannelHandler> T replace( final Class<T> oldHandlerType, final String newName, final ChannelHandler newHandler ) {
          return this.delegate.replace( oldHandlerType, newName, newHandler );
        }
        
        @Override
        public ChannelHandler getFirst( ) {
          return this.delegate.getFirst( );
        }
        
        @Override
        public ChannelHandler getLast( ) {
          return this.delegate.getLast( );
        }
        
        @Override
        public ChannelHandler get( final String name ) {
          return this.delegate.get( name );
        }
        
        @Override
        public <T extends ChannelHandler> T get( final Class<T> handlerType ) {
          return this.delegate.get( handlerType );
        }
        
        @Override
        public ChannelHandlerContext getContext( final ChannelHandler handler ) {
          return this.delegate.getContext( Handlers.wrapHandler( handler ) );
        }
        
        @Override
        public ChannelHandlerContext getContext( final String name ) {
          return this.delegate.getContext( name );
        }
        
        @Override
        public ChannelHandlerContext getContext( final Class<? extends ChannelHandler> handlerType ) {
          return this.delegate.getContext( handlerType );
        }
        
        @Override
        public void sendUpstream( final ChannelEvent e ) {
          this.delegate.sendUpstream( e );
        }
        
        @Override
        public void sendDownstream( final ChannelEvent e ) {
          this.delegate.sendDownstream( e );
        }
        
        @Override
        public Channel getChannel( ) {
          return this.delegate.getChannel( );
        }
        
        @Override
        public ChannelSink getSink( ) {
          return this.delegate.getSink( );
        }
        
        @Override
        public void attach( final Channel channel, final ChannelSink sink ) {
          this.delegate.attach( channel, sink );
        }
        
        @Override
        public boolean isAttached( ) {
          return this.delegate.isAttached( );
        }
        
        @Override
        public Map<String, ChannelHandler> toMap( ) {
          return this.delegate.toMap( );
        }
        
      };
      pipeline.addLast( "ssl", Handlers.newSslHandler( ) );
      pipeline.addLast( "decoder", new NioHttpDecoder( ) );
      pipeline.addLast( "encoder", new HttpResponseEncoder( ) );
      pipeline.addLast( "chunkedWriter", new ChunkedWriteHandler( ) );
      pipeline.addLast( "bootstrap-fence", Handlers.bootstrapFence( ) );
      pipeline.addLast( "handler", new NioServerHandler( ) );
      return pipeline;
    }
  }
  
  public static ChannelPipelineFactory serverPipelineFactory( ) {
    return ServerPipelineFactory.INSTANCE;
  }
  
  public static class NioHttpResponseDecoder extends HttpResponseDecoder {
    
    @Override
    protected HttpMessage createMessage( final String[] strings ) {
      return new MappingHttpResponse( strings );//HttpVersion.valueOf(strings[2]), HttpMethod.valueOf(strings[0]), strings[1] );
    }
  }
  
  @ChannelPipelineCoverage( "all" )
  public static class NioHttpRequestEncoder extends HttpMessageEncoder {
    
    public NioHttpRequestEncoder( ) {
      super( );
    }
    
    @Override
    protected void encodeInitialLine( final ChannelBuffer buf, final HttpMessage message ) throws Exception {
      final MappingHttpRequest request = ( MappingHttpRequest ) message;
      buf.writeBytes( request.getMethod( ).toString( ).getBytes( "ASCII" ) );
      buf.writeByte( HttpUtils.SP );
      buf.writeBytes( request.getServicePath( ).getBytes( "ASCII" ) );
      buf.writeByte( HttpUtils.SP );
      buf.writeBytes( request.getProtocolVersion( ).toString( ).getBytes( "ASCII" ) );
      buf.writeBytes( HttpUtils.CRLF );
    }
  }
  
  @ChannelPipelineCoverage( "all" )
  enum BootstrapStateCheck implements ChannelUpstreamHandler {
    INSTANCE;
    
    @Override
    public void handleUpstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
      if ( !Bootstrap.isFinished( ) ) {
        //TODO:GRZE: do nothing for the moment, not envouh info here.
//        throw new ServiceNotReadyException( "System has not yet completed booting." );
        ctx.sendUpstream( e );
      } else {
        ctx.sendUpstream( e );
      }
    }
    
  }
  
  public static ChannelHandler bootstrapFence( ) {
    return BootstrapStateCheck.INSTANCE;
  }
  
  public static Map<String, ChannelHandler> channelMonitors( final TimeUnit unit, final int timeout ) {
    return new HashMap<String, ChannelHandler>( 4 ) {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      {
//        put( "state-monitor", new ChannelStateMonitor( ) );
        this.put( "idlehandler", new IdleStateHandler( Handlers.timer, timeout, timeout, timeout, unit ) );
        this.put( "readTimeout", new ReadTimeoutHandler( Handlers.timer, timeout, unit ) );
        this.put( "writeTimeout", new WriteTimeoutHandler( Handlers.timer, timeout, unit ) );
      }
    };
  }
  
  public static ChannelHandler newSslHandler( ) {
    return new NioSslHandler( );
  }
  
  public static ChannelHandler newHttpResponseDecoder( ) {
    return new NioHttpResponseDecoder( );
  }
  
  public static ChannelHandler newHttpChunkAggregator( ) {
    return new HttpChunkAggregator( StackConfiguration.CLIENT_HTTP_CHUNK_BUFFER_MAX );
  }
  
  public static ChannelHandler addressingHandler( ) {//caching
    return addressingHandler;
  }
  
  public static ChannelHandler newAddressingHandler( final String addressingPrefix ) {//caching
    return new AddressingHandler( addressingPrefix );
  }
  
  public static ChannelHandler bindingHandler( final String bindingName ) {
    if ( bindingHandlers.containsKey( bindingName ) ) {
      return bindingHandlers.get( bindingName );
    } else {
      String maybeBindingName = "";
      if ( BindingManager.isRegisteredBinding( bindingName ) ) {
        bindingHandlers.putIfAbsent( bindingName, new BindingHandler( BindingManager.getBinding( bindingName ) ) );
      } else if ( BindingManager.isRegisteredBinding( maybeBindingName = BindingManager.sanitizeNamespace( bindingName ) ) ) {
        bindingHandlers.putIfAbsent( bindingName, new BindingHandler( BindingManager.getBinding( maybeBindingName ) ) );
      } else {
        throw Exceptions.trace( "Failed to find registerd binding for name: " + bindingName
                                + ".  Also tried looking for sanitized name: "
                                + maybeBindingName );
      }
      return bindingHandlers.get( bindingName );
    }
  }
  
  public static ChannelHandler httpRequestEncoder( ) {
    return httpRequestEncoder;
  }
  
  public static ChannelHandler soapMarshalling( ) {
    return soapMarshallingHandler;
  }
  
  public static ChannelHandler soapHandler( ) {
    return soapHandler;
  }
  
  @ChannelPipelineCoverage( "one" )
  private static class NioSslHandler extends SslHandler {
    private final AtomicBoolean first = new AtomicBoolean( true );
    
    NioSslHandler( ) {
      super( SslSetup.getServerEngine( ) );
    }
    
    private static List<String> httpVerbPrefix = Lists.newArrayList( HttpMethod.CONNECT.getName( ).substring( 0, 3 ),
                                                                     HttpMethod.GET.getName( ).substring( 0, 3 ),
                                                                     HttpMethod.PUT.getName( ).substring( 0, 3 ),
                                                                     HttpMethod.POST.getName( ).substring( 0, 3 ),
                                                                     HttpMethod.HEAD.getName( ).substring( 0, 3 ),
                                                                     HttpMethod.OPTIONS.getName( ).substring( 0, 3 ),
                                                                     HttpMethod.DELETE.getName( ).substring( 0, 3 ),
                                                                     HttpMethod.TRACE.getName( ).substring( 0, 3 ) );
    
    private static boolean maybeSsl( final ChannelBuffer buffer ) {
      buffer.markReaderIndex( );
      final StringBuffer sb = new StringBuffer( );
      for ( int lineLength = 0; lineLength++ < 3; sb.append( ( char ) buffer.readByte( ) ) );
      buffer.resetReaderIndex( );
      return !httpVerbPrefix.contains( sb.toString( ) );
    }
    
    @Override
    public void handleUpstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
      Object o = null;
      if ( ( e instanceof MessageEvent )
           && this.first.compareAndSet( true, false )
           && ( ( o = ( ( MessageEvent ) e ).getMessage( ) ) instanceof ChannelBuffer )
           && !maybeSsl( ( ChannelBuffer ) o ) ) {
        ctx.getPipeline( ).removeFirst( );
        ctx.sendUpstream( e );
      } else {
        super.handleUpstream( ctx, e );
      }
    }
    
  }
  
  public static ChannelHandler internalServiceStateHandler( ) {
    return MessageEpochChecks.INSTANCE;
  }
  
  @ChannelPipelineCoverage( "all" )
  public enum ServiceStateChecksHandler implements ChannelUpstreamHandler {
    INSTANCE {
      @Override
      public void handleUpstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
        final MappingHttpRequest request = MappingHttpMessage.extractMessage( e );
        final BaseMessage msg = BaseMessage.extractMessage( e );
        if ( msg != null ) {
          try {
            final Class<? extends ComponentId> compClass = ComponentMessages.lookup( msg );
            if ( Topology.isEnabledLocally( compClass ) ) {
              ctx.sendUpstream( e );
            } else {
              Handlers.sendError( ctx, e, compClass, request.getServicePath( ) );
            }
          } catch ( final NoSuchElementException ex ) {
            LOG.warn( "Failed to find reverse component mapping for message type: " + msg.getClass( ) );
            ctx.sendUpstream( e );
          } catch ( final Exception ex ) {
            Logs.extreme( ).error( ex, ex );
            ctx.sendUpstream( e );
          }
        } else {
          ctx.sendUpstream( e );
        }
      }
      
    }
  }
  
  public static ChannelHandler internalEpochHandler( ) {
    return MessageEpochChecks.INSTANCE;
  }
  
  @ChannelPipelineCoverage( "all" )
  enum MessageEpochChecks implements ChannelUpstreamHandler {
    INSTANCE {
      @Override
      public void handleUpstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
        final MappingHttpRequest request = MappingHttpMessage.extractMessage( e );
        final BaseMessage msg = BaseMessage.extractMessage( e );
        if ( msg != null ) {
          try {
            if ( msg instanceof ServiceTransitionType ) {
              Topology.touch( ( ServiceTransitionType ) msg );
              ctx.sendUpstream( e );
            } else if ( Topology.check( msg ) ) {
              ctx.sendUpstream( e );
            } else {
              final Class<? extends ComponentId> compClass = ComponentMessages.lookup( msg );
              Handlers.sendError( ctx, e, compClass, request.getServicePath( ) );
            }
          } catch ( final Exception ex ) {
            Logs.extreme( ).error( ex, ex );
            ctx.sendUpstream( e );
          }
        }
      }
    };
    
  }
  
  static void sendError( final ChannelHandlerContext ctx, final ChannelEvent e, final Class<? extends ComponentId> compClass, final String originalPath ) {
    e.getFuture( ).cancel( );
    HttpResponse response = null;
    if ( !Topology.enabledServices( compClass ).isEmpty( ) ) {
      response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.TEMPORARY_REDIRECT );
      final URI serviceUri = ServiceUris.remote( Topology.lookup( compClass ) );
      final String redirectUri = serviceUri.toASCIIString( ) + originalPath.replace( serviceUri.getPath( ), "" );
      response.setHeader( HttpHeaders.Names.LOCATION, redirectUri );
    } else {
      response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND );
    }
    final ChannelFuture writeFuture = Channels.future( ctx.getChannel( ) );
    writeFuture.addListener( ChannelFutureListener.CLOSE );
    if ( ctx.getChannel( ).isConnected( ) ) {
      Channels.write( ctx, writeFuture, response );
    }
  }
  
  public static ChannelHandler internalOnlyHandler( ) {
    return InternalOnlyHandler.INSTANCE;
  }
  
  @ChannelPipelineCoverage( "all" )
  enum InternalOnlyHandler implements ChannelUpstreamHandler {
    INSTANCE;
    @Override
    public void handleUpstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
      final MappingHttpMessage request = MappingHttpMessage.extractMessage( e );
      final BaseMessage msg = BaseMessage.extractMessage( e );
      if ( ( request != null ) && ( msg != null ) ) {
        final User user = Contexts.lookup( request.getCorrelationId( ) ).getUser( );
        if ( user.isSystemAdmin( ) ) {
          ctx.sendUpstream( e );
        } else {
          Contexts.clear( Contexts.lookup( msg.getCorrelationId( ) ) );
          ctx.getChannel( ).write( new MappingHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.FORBIDDEN ) );
        }
      } else {
        ctx.sendUpstream( e );
      }
    }
    
  }
  
}
