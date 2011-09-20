/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.client;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.EucalyptusClusterException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.Client;
import com.eucalyptus.ws.handlers.ResponseHandler;
import com.eucalyptus.ws.util.NioBootstrap;
import com.google.common.base.Supplier;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class NioClient implements Client {
  public interface ResponseHandlingPipeline extends ChannelPipelineFactory, Supplier<ResponseHandler> {};
  private static class DeferedWriter implements ChannelFutureListener {
    private Object                request;
    private ChannelFutureListener callback;
    
    DeferedWriter( final Object request, final ChannelFutureListener callback ) {
      this.callback = callback;
      this.request = request;
    }
    
    @Override
    public void operationComplete( ChannelFuture channelFuture ) {
      if ( channelFuture.isSuccess( ) ) {
        channelFuture.getChannel( ).write( request ).addListener( callback );
      } else {
        LOG.debug( channelFuture.getCause( ), channelFuture.getCause( ) );
        try {
          callback.operationComplete( channelFuture );
        } catch ( Exception e ) {
          LOG.debug( e, e );
        }
      }
    }
    
  }
  @ChannelPipelineCoverage( "one" )
  public class NioResponseHandler extends SimpleChannelHandler implements ResponseHandler {
    private Lock canHas = new ReentrantLock();
    private Condition ready = canHas.newCondition( );
    private AtomicReference<Object> response = new AtomicReference<Object>(null);
    protected BlockingQueue<BaseMessage> requestQueue = new LinkedBlockingQueue<BaseMessage>( );

    public boolean hasException( ) {
      return this.response.get() instanceof Throwable;
    }
    
    public boolean hasResponse( ) {
      return this.response.get() instanceof BaseMessage;
    }
    
    public BaseMessage getResponse( ) throws Exception {
      this.waitForResponse( );
      if( this.response.get( ) instanceof BaseMessage ) {
        return (BaseMessage) this.response.get( );
      } else if ( this.response.get() instanceof Throwable ) {
        throw new EucalyptusClusterException( "Exception in NIO request.", (Throwable) this.response.get( ) );
      }
      throw new EucalyptusClusterException( "Failed to retrieve result of asynchronous operation." );
    }

    public void exceptionCaught( final ChannelHandlerContext ctx, final Throwable e ) {
      LOG.debug( e, e );
      this.queueResponse( e );
    }

    @Override
    public void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent e ) {
      this.exceptionCaught( ctx, e.getCause( ) );
      ctx.getChannel( ).close( );
    }
    
    @Override
    public void messageReceived( final ChannelHandlerContext ctx, final MessageEvent e ) throws Exception {
      final MappingHttpMessage httpResponse = ( MappingHttpMessage ) e.getMessage( );
      final BaseMessage reply = ( BaseMessage ) httpResponse.getMessage( );
      this.queueResponse( reply );
      ctx.getChannel( ).close( );
    }

    public void queueResponse( Object o ) {
      if ( o instanceof MappingHttpResponse ) {
        MappingHttpResponse httpResponse = (MappingHttpResponse) o;
        if( httpResponse.getMessage( ) != null ) {
          o = httpResponse.getMessage( );
        } else {
          o = new EucalyptusClusterException( httpResponse.getMessageString( ) );
        }
      }
      this.canHas.lock( );
      try {
        if( !this.response.compareAndSet( null, o ) ) {
          if( !( o instanceof Throwable ) ) {
            LOG.debug( LogUtil.subheader( "Received spurious second response: " + LogUtil.dumpObject( o ) ) );
          }
          o = this.response.getAndSet( o );
          LOG.debug( LogUtil.subheader( "Previous response was: " + LogUtil.dumpObject( this.response.get( ) ) ) );
        } else {
          if( o instanceof Throwable ) {
            LOG.error( "Caught exception in asynchronous response handler.", (Throwable) o );
          } else {
            LOG.trace( this.getClass( ).getSimpleName( ) + " Got response of: " + LogUtil.dumpObject( o ) );
          }
        }
        this.ready.signalAll( );
      } finally {
        this.canHas.unlock( );
      }
    }

    public Throwable getException( ) {
      this.waitForResponse();
      return (Throwable) this.response.get( );
    }

    public void waitForResponse( ) {
      this.canHas.lock( );
      try {
        while( this.response.get( ) == null ) {
          try {
            this.ready.await( 10000, TimeUnit.MILLISECONDS );
            LOG.debug( "Waiting for response." );
          } catch ( InterruptedException e ) {
            LOG.debug( e, e );
            Thread.currentThread( ).interrupt( );
          }
        }
        EventRecord.here( NioResponseHandler.class, EventType.MSG_SERVICED, this.response.get().getClass( ).toString( ) ).debug( );
      } finally {
        this.canHas.unlock( );
      }
    }

    @Override
    public void channelClosed( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
      if( this.response.get( ) == null ) {
        this.queueResponse( new EucalyptusClusterException( LogUtil.dumpObject( e ) ) );
      }
      super.channelClosed( ctx, e );
    }

    @Override
    public BaseMessage getRequest( ) {
      throw new RuntimeException( "Not implemented" );
    }
    
  }

  private static NioClientSocketChannelFactory clientSocketFactory;

  private static OrderedMemoryAwareThreadPoolExecutor clientBossPool;

  private static OrderedMemoryAwareThreadPoolExecutor clientWorkerPool;
  public static NioBootstrap getClientBootstrap( ChannelPipelineFactory factory ) {
    final NioBootstrap bootstrap = new NioBootstrap( getClientChannelFactory( ) );//TODO: pass port host, etc here.
    bootstrap.setPipelineFactory( factory );
    bootstrap.setOption( "tcpNoDelay", false );
    bootstrap.setOption( "keepAlive", false );
    bootstrap.setOption( "reuseAddress", false );
    bootstrap.setOption( "connectTimeoutMillis", 3000 );
    return bootstrap;
  }

  private static synchronized ChannelFactory getClientChannelFactory( ) {
    if ( clientSocketFactory == null ) {
      clientBossPool = new OrderedMemoryAwareThreadPoolExecutor( 4, 10485760l, 200 * 1024 * 1024l, 500l, TimeUnit.MILLISECONDS );
      clientWorkerPool = new OrderedMemoryAwareThreadPoolExecutor( 16, 10485760l, 200 * 1024 * 1024l, 500l, TimeUnit.MILLISECONDS );
      clientSocketFactory = new NioClientSocketChannelFactory( clientBossPool, clientWorkerPool, 16 );
    }
    return clientSocketFactory;
  }

  public static ChannelFutureListener WRITE( Object o ) {
    return new DeferedWriter( o, new ChannelFutureListener( ) {
      public void operationComplete( ChannelFuture future ) throws Exception {}
    } );
  }
  
  public static class DelegatingPipeline implements ResponseHandlingPipeline {
    private ChannelPipelineFactory pipelineFactory;
    private ResponseHandler        handler;
    DelegatingPipeline( ChannelPipelineFactory pipelineFactory, ResponseHandler handler ) {
      super( );
      this.pipelineFactory = pipelineFactory;
      this.handler = handler;
    }
    public ChannelPipeline getPipeline( ) throws Exception {
      ChannelPipeline pipeline = this.pipelineFactory.getPipeline( );
      pipeline.addLast( "response-handler", handler );
      return pipeline;
    }
    public BaseMessage getRequest( ) {
      return this.handler.getRequest( );
    }
    public BaseMessage getResponse( ) throws Exception {
      return this.handler.getResponse( );
    }
    public void waitForResponse( ) {
      this.handler.waitForResponse( );
    }
    @Override
    public ResponseHandler get( ) {
      return this.handler;
    }
    
  }
  
  private static Logger     LOG = Logger.getLogger( NioClient.class );
  
  private NioBootstrap      clientBootstrap;
  
  private String            hostname;
  private int               port;
  private String            servicePath;
  private InetSocketAddress remoteAddr;
  private ResponseHandler   responseHandler;
  private ChannelFuture     connectFuture;
  
  
  public NioClient( String hostname, int port, String servicePath, ChannelPipelineFactory clientPipeline ) {
    this.responseHandler = new NioResponseHandler( );
    this.clientBootstrap = getClientBootstrap( new DelegatingPipeline( clientPipeline, this.responseHandler ) );
    this.remoteAddr = new InetSocketAddress( hostname, port );
    this.hostname = hostname;
    this.port = port;
    this.servicePath = servicePath;
    this.connectFuture = this.clientBootstrap.connect( this.remoteAddr );
  }
  
  public void write( HttpRequest httpRequest ) throws Exception {
    this.connectFuture.addListener( WRITE( httpRequest ) );
  }
  
  @Override
  public BaseMessage send( final BaseMessage msg ) throws Exception {
    HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, this.hostname, this.port, this.servicePath, msg );
    this.write( request );
    BaseMessage response = this.responseHandler.getResponse( );
    return response;
  }
  
  @Override
  public void dispatch( final BaseMessage msg ) throws Exception {
    HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, this.hostname, this.port, this.servicePath, msg );
    this.write( request );
    BaseMessage response = this.responseHandler.getResponse( );
  }
  
  @Override
  public String getUri( ) {
    return "http://" + this.hostname + ":" + this.port + ( servicePath.startsWith( "/" )
      ? ""
      : "?" ) + servicePath;
  }
  
  public String getHostname( ) {
    return hostname;
  }
  
  public int getPort( ) {
    return port;
  }
  
  public String getServicePath( ) {
    return servicePath;
  }
  
}
