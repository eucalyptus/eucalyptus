/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.WildcardNameMatcher;
import com.eucalyptus.util.async.AsyncRequestChannelPoolMap.ChannelPoolKey;
import com.eucalyptus.ws.EucalyptusRemoteFault;
import com.eucalyptus.ws.IoMessage;
import com.eucalyptus.ws.StackConfiguration;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * @author decker
 * @param <Q>
 * @param <R>
 */
public class AsyncRequestHandler<Q extends BaseMessage, R extends BaseMessage> extends ChannelInboundHandlerAdapter implements RequestHandler<Q, R> {
  private static final Logger                LOG           = Logger.getLogger( AsyncRequestHandler.class );
  private static final Logger                MESSAGE_LOG   = Logger.getLogger( "com.eucalyptus.client.MessageLogger" );
  private static final WildcardNameMatcher   MATCHER       = new WildcardNameMatcher( );
  private static final AsyncRequestChannelPoolMap POOL_MAP = new AsyncRequestChannelPoolMap( );

  private final AsyncRequest<Q, R>         parent;
  private final AtomicBoolean              writeComplete   = new AtomicBoolean( false );
  private final AtomicBoolean              channelReleased = new AtomicBoolean( false );
  private final AtomicReference<Q>         request         = new AtomicReference<>( null );
  private final CheckedListenableFuture<R> response;

  private volatile ChannelPool     channelPool;
  private volatile Future<Channel> acquireFuture;

  AsyncRequestHandler( final AsyncRequest<Q, R> parent, final CheckedListenableFuture<R> response ) {
    super( );
    this.parent = parent;
    this.response = response;
  }
  
  /**
   *
   */
  @Override
  public boolean fire( final ServiceConfiguration config, final Q request ) {
    if ( !this.request.compareAndSet( null, request ) ) {
      LOG.warn( "Duplicate write attempt for request: " + this.request.get( ).getClass( ).getSimpleName( ) );
      return false;
    } else {
      try {
        final InetSocketAddress serviceSocketAddress = config.getSocketAddress( );
        final Bootstrap clientBootstrap = config.getComponentId( ).getClientBootstrap( );
        final ChannelInitializer<?> initializer = config.getComponentId( ).getClientChannelInitializer( );
        final int poolSizeLimit = initializer instanceof AsyncRequestPoolable ?
            ((AsyncRequestPoolable)initializer).fixedSize() :
            -1;
        final IoMessage<FullHttpRequest> ioMessage =
            IoMessage.httpRequest( ServiceUris.internal( config ), this.request.get( ) );
        final ChannelPoolKey poolKey =
            new ChannelPoolKey( clientBootstrap, initializer, serviceSocketAddress, poolSizeLimit );
        final long before = System.currentTimeMillis( );
        this.channelPool = POOL_MAP.get( poolKey );
        this.acquireFuture = channelPool.acquire( );
        this.acquireFuture.addListener( new GenericFutureListener<Future<Channel>>( ) {
          @Override
          public void operationComplete( final Future<Channel> future ) throws Exception {
            try {
              if ( future.isSuccess( ) ) {
                final Channel channel = future.get( );
                logAcquired( channel, before );
                channel.pipeline( ).addLast( "request-handler", AsyncRequestHandler.this );

                if ( !initializer.getClass( ).getSimpleName( ).startsWith( "GatherLog" ) ) {
                  Topology.populateServices( config, AsyncRequestHandler.this.request.get( ) );
                }

                logMessage( ioMessage );

                channel.writeAndFlush( ioMessage ).addListener( new ChannelFutureListener( ) {
                  @Override
                  public void operationComplete( final ChannelFuture future ) throws Exception {
                    AsyncRequestHandler.this.writeComplete.set( true );
                    
                    Logs.extreme( ).debug(
                      EventRecord.here(
                        request.getClass( ),
                        EventClass.SYSTEM_REQUEST,
                        EventType.CHANNEL_WRITE,
                        request.getClass( ).getSimpleName( ),
                        request.getCorrelationId( ),
                        serviceSocketAddress.toString( ),
                        "" + future.channel( ).localAddress( ),
                        "" + future.channel( ).remoteAddress( ) ) );
                  }
                } );
              } else {
                AsyncRequestHandler.this.teardown( future.cause( ) );
              }
            } catch ( final Exception ex ) {
              LOG.error( ex, ex );
              AsyncRequestHandler.this.teardown( ex );
            }
          }
        } );
        return true;
      } catch ( final Exception t ) {
        LOG.error( t, t );
        this.teardown( t );
        return false;
      }
    }
  }

  private void logAcquired( final Channel channel, final long before ) {
    final long acquireTime = System.currentTimeMillis( ) - before;
    final Level level;
    if ( acquireTime > 45_000L ) {
      level = Level.WARN;
    } else if ( acquireTime > 30_000L ) {
      level = Level.INFO;
    } else if ( acquireTime > 10_000L ) {
      level = Level.DEBUG;
    } else {
      level = Level.TRACE;
    }
    if ( LOG.isEnabledFor( level ) ) {
      LOG.log( level, "Acquire took " + acquireTime + "ms for " + channel.remoteAddress( ) );
    }
    Logs.extreme( ).debug( "Acquired as: " + channel.localAddress( ) );
  }

  private void logMessage( final IoMessage ioMessage ) {
    Logs.extreme( ).debug( ioMessage );
    final Object payload = ioMessage.getMessage( );
    final String patternList = Objects.toString( StackConfiguration.CLIENT_MESSAGE_LOG_WHITELIST, "" );
    if ( payload != null && (
        MATCHER.matches( patternList, payload.getClass( ).getSimpleName( ) ) ||
        MATCHER.matches( patternList, payload.getClass( ).getName( ) ) ) ) {
      MESSAGE_LOG.info( payload );
    }
  }

  private void teardown( Throwable t ) {
    if ( t == null ) {
      t = new NullPointerException( "teardown() called with null argument." );
    }
    this.logRequestFailure( t );
    this.response.setException( t );
    if ( this.acquireFuture != null ) {
      this.maybeCloseChannel( );
    }
  }

  private void maybeCloseChannel( ) {
    if ( !this.acquireFuture.isDone( ) &&  !this.acquireFuture.cancel(true) ) {
      LOG.error( "Failed to cancel in-flight connection request: " + this.acquireFuture.getNow( ).toString( ) );
    }
    final Channel channel = this.acquireFuture.getNow( );
    if ( channel != null ) {
      closeAndReleaseChannel( channel );
    }
  }

  private void closeAndReleaseChannel( @Nonnull final Channel channel ) {
    if ( channel.isOpen( ) ) {
      channel.close( ).addListener( new ChannelFutureListener( ) {
        @Override
        public void operationComplete( final ChannelFuture future ) throws Exception {
          EventRecord.here( AsyncRequestHandler.this.request.get( ).getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_CLOSED ).trace( );
          releaseChannel( channel );
        }
      } );
    } else {
      EventRecord.here( AsyncRequestHandler.this.request.get( ).getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_CLOSED, "ALREADY_CLOSED" ).trace( );
      releaseChannel( channel );
    }
  }

  private void releaseChannel( @Nonnull final Channel channel ) {
    if ( this.channelPool != null && channelReleased.compareAndSet( false, true )) {
      this.channelPool.release( channel );
    }
  }

  private void logRequestFailure( Throwable t ) {
    try {
      Logs.extreme( ).debug( "RESULT:" + t.getMessage( )
                 + ":REQUEST:"
                 + ( ( this.request.get( ) != null )
                   ? this.request.get( ).getClass( )
                   : "REQUEST IS NULL" ) );
      if ( Exceptions.isCausedBy( t, RetryableConnectionException.class ) 
          || Exceptions.isCausedBy( t, ConnectionException.class )
          || Exceptions.isCausedBy( t, IOException.class ) ) {
        Logs.extreme( ).debug( "Failed request: " + this.request.get( ).toSimpleString( ) + " because of: " + t.getMessage( ), t );
      }
    } catch ( Exception ex ) {
      Logs.extreme( ).error( ex , ex );
    }
  }

  @Override
  public void channelInactive( final ChannelHandlerContext ctx ) throws Exception {
    this.checkFinished( ctx, true );
    super.channelInactive( ctx );
  }

  @Override
  public void channelRead( final ChannelHandlerContext ctx, final Object msg ) throws Exception {
    this.messageReceived( ctx, msg );
  }

  @Override
  public void exceptionCaught( final ChannelHandlerContext ctx, final Throwable cause ) throws Exception {
    Logs.extreme( ).error( cause, cause );
    if ( cause instanceof EucalyptusRemoteFault ) {//GRZE: treat this like a normal response, set the response and close the channel.
      this.response.setException( cause );
      if ( this.acquireFuture != null ) {
        this.maybeCloseChannel( );
      }
    } else {
      this.teardown( cause );
    }
  }

  private void messageReceived( final ChannelHandlerContext ctx, final Object message ) {
    try {
      if ( message instanceof IoMessage ) {
        final IoMessage response = ( IoMessage ) message;
        try {
          final R msg = ( R ) response.getMessage( );
          if ( !msg.get_return( true ) ) {
            this.teardown( new FailedRequestException( "Cluster response includes _return=false", msg ) );
          } else {
            logMessage( response );
            this.response.set( msg );
            if ( HttpHeaders.isKeepAlive( ((IoMessage) message).getHttpMessage( ) ) ) {
              releaseChannel( ctx.channel( ) );
            } else {
              closeAndReleaseChannel( ctx.channel( ) );
            }
          }
        } catch ( final Exception e1 ) {
          LOG.error( e1, e1 );
          this.teardown( e1 );
        }
      } else if ( message == null ) {
        final NoResponseException ex = new NoResponseException( "Channel received a null response.", this.request.get( ) );
        LOG.error( ex, ex );
        this.teardown( ex );
      } else {
        final UnknownMessageTypeException ex =
            new UnknownMessageTypeException( "Channel received a unknown response type: " +
                message.getClass( ).getCanonicalName( ), this.request.get( ), message );
        LOG.error( ex, ex );
        this.teardown( ex );
      }
    } catch ( final Exception t ) {
      LOG.error( t, t );
      this.teardown( t );
    } finally {
      if ( message instanceof IoMessage ) {
        ((IoMessage)message).getHttpMessage( ).release( );
      }
    }
  }
  
  private void checkFinished( final ChannelHandlerContext ctx, final boolean inactive ) {
    if ( ( this.acquireFuture != null ) && !this.acquireFuture.isSuccess( )
         && ( this.acquireFuture.cause( ) instanceof IOException ) ) {
      final Throwable ioError = this.acquireFuture.cause( );
      if ( !this.writeComplete.get( ) ) {
        this.teardown( new RetryableConnectionException( "Channel was closed before the write operation could be completed: " + ioError.getMessage( ), ioError,
                                                         this.request.get( ) ) );
      } else {
        this.teardown( new ConnectionException( "Channel was closed before the response was received: " + ioError.getMessage( ), ioError, this.request.get( ) ) );
      }
    } else {
      if ( !this.writeComplete.get( ) ) {
        this.teardown( new RetryableConnectionException( "Channel was closed before the write operation could be completed", this.request.get( ) ) );
      } else if ( !this.response.isDone( ) ) {
        this.teardown( new ConnectionException( "Channel was closed before the response was received.", this.request.get( ) ) );
      } else {
        //GRZE:WOO:HA: guess we either failed to connect asynchronously or did the write but didn't actually read anything. So....
        this.teardown( new ChannelException( "Channel was closed before connecting." ) );
      }
    }
  }
  
  public AtomicReference<Q> getRequest() {
    return request;
  }

  public CheckedListenableFuture<R> getResponse() {
    return response;
  }

}
