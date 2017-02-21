/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util.async;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.EucalyptusRemoteFault;
import com.eucalyptus.ws.IoMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

/**
 * @author decker
 * @param <Q>
 * @param <R>
 */
public class AsyncRequestHandler<Q extends BaseMessage, R extends BaseMessage> extends ChannelInboundHandlerAdapter implements RequestHandler<Q, R> {
  private static Logger                LOG           = Logger.getLogger( AsyncRequestHandler.class );
  private final AsyncRequest<Q, R> parent;

  private Bootstrap                    clientBootstrap;
  private ChannelFuture                connectFuture;
  
  private final AtomicBoolean          writeComplete = new AtomicBoolean( false );
  private final CheckedListenableFuture<R>   response;
  private transient AtomicReference<Q> request       = new AtomicReference<Q>( null );
  
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
      final SocketAddress serviceSocketAddress = config.getSocketAddress( );
      final ChannelInitializer<?> initializer = config.getComponentId( ).getClientChannelInitializer( );
      try {
        this.clientBootstrap = config.getComponentId( ).getClientBootstrap( )
          .handler( new ChannelInitializer<SocketChannel>( ) {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
              ch.pipeline( ).addLast( "initializer", initializer );
              ch.pipeline( ).addLast( "async-initializer", new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel( SocketChannel ch ) throws Exception {
                  ch.pipeline( ).addLast( "request-handler", AsyncRequestHandler.this );
                }
              } );
            }
          } );
//TODO:GRZE: better logging here        LOG.debug( request.getClass( ).getSimpleName( ) + ":" + request.getCorrelationId( ) + " connecting to " + serviceSocketAddress );
        Logs.extreme( ).debug( EventRecord.here( request.getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_OPENING, request.getClass( ).getSimpleName( ),
                          request.getCorrelationId( ), serviceSocketAddress.toString( ) ) );
        final IoMessage<FullHttpRequest> ioMessage =
            IoMessage.httpRequest( ServiceUris.internal( config ), this.request.get( ) );

        this.connectFuture = this.clientBootstrap.connect( serviceSocketAddress );
        this.connectFuture.addListener( new ChannelFutureListener( ) {
          @Override
          public void operationComplete( final ChannelFuture future ) throws Exception {
            try {
              if ( future.isSuccess( ) ) {
                Logs.extreme( ).debug( "Connected as: " + future.channel( ).localAddress( ) );
                
                final InetAddress localAddr = ( ( InetSocketAddress ) future.channel( ).localAddress( ) ).getAddress( );
                if ( !initializer.getClass( ).getSimpleName( ).startsWith( "GatherLog" ) ) {
                  Topology.populateServices( config, AsyncRequestHandler.this.request.get( ) );
                }

                Logs.extreme( ).debug(
                  EventRecord.here(
                    request.getClass( ),
                    EventClass.SYSTEM_REQUEST,
                    EventType.CHANNEL_OPEN,
                    request.getClass( ).getSimpleName( ),
                    request.getCorrelationId( ),
                    serviceSocketAddress.toString( ),
                    "" + future.channel( ).localAddress( ),
                    "" + future.channel( ).remoteAddress( ) ) );
                Logs.extreme( ).debug( ioMessage );
                
                future.channel( ).writeAndFlush( ioMessage ).addListener( new ChannelFutureListener( ) {
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
  
  private void teardown( Throwable t ) {
    if ( t == null ) {
      t = new NullPointerException( "teardown() called with null argument." );
    }
    this.logRequestFailure( t );
    this.response.setException( t );
    if ( this.connectFuture != null ) {
      this.maybeCloseChannel( );
    }
  }

  private void maybeCloseChannel( ) {
    if ( this.connectFuture.isDone( ) && this.connectFuture.isSuccess( ) ) {
      final Channel channel = this.connectFuture.channel( );
      if ( ( channel != null ) && channel.isOpen( ) ) {
        channel.close( ).addListener( new ChannelFutureListener( ) {
          @Override
          public void operationComplete( final ChannelFuture future ) throws Exception {
            EventRecord.here( AsyncRequestHandler.this.request.get( ).getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_CLOSED ).trace( );
          }
        } );
      } else {
        EventRecord.here( AsyncRequestHandler.this.request.get( ).getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_CLOSED, "ALREADY_CLOSED" ).trace( );
      }
    } else if ( !this.connectFuture.isDone( ) && !this.connectFuture.cancel(true) ) {
      LOG.error( "Failed to cancel in-flight connection request: " + this.connectFuture.toString( ) );
      final Channel channel = this.connectFuture.channel( );
      if ( channel != null ) {
        channel.close( );
      }
    } else if ( !this.connectFuture.isSuccess( ) ) {
      final Channel channel = this.connectFuture.channel( );
      if ( channel != null ) {
        channel.close( );
      }
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
      if ( this.connectFuture != null ) {
        this.maybeCloseChannel( );
      }
    } else {
      this.teardown( cause );
    }
    super.exceptionCaught( ctx, cause );
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
            this.response.set( msg );
          }
          //TODO:STEVE: was a future close here  ctx.addListener( ChannelFutureListener.CLOSE )
          if ( HttpHeaders.isKeepAlive( ((IoMessage) message).getHttpMessage( ) ) ) {
            //TODO:STEVE: return to pool?
          } // else
          ctx.close( );
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
    }
  }
  
  private void checkFinished( final ChannelHandlerContext ctx, final boolean inactive ) {
    if ( ( this.connectFuture != null ) && !this.connectFuture.isSuccess( )
         && ( this.connectFuture.cause( ) instanceof IOException ) ) {
      final Throwable ioError = this.connectFuture.cause( );
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

  public AtomicBoolean getWriteComplete() {
    return writeComplete;
  }

  public ChannelFuture getConnectFuture() {
    return connectFuture;
  }

  public Bootstrap getClientBootstrap() {
    return clientBootstrap;
  }

}
