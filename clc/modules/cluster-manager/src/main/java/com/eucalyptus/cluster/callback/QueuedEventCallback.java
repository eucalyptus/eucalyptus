/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/**
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.cluster.callback;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.FailureCallback;
import com.eucalyptus.cluster.SuccessCallback;
import com.eucalyptus.cluster.UnconditionalCallback;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.EucalyptusClusterException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.client.NioBootstrap;
import com.eucalyptus.ws.client.pipeline.ClusterClientPipeline;
import com.eucalyptus.ws.client.pipeline.NioClientPipeline;
import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.handlers.ResponseHandler;
import com.eucalyptus.ws.util.ChannelUtil;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import com.eucalyptus.records.EventRecord;

@SuppressWarnings( "unchecked" )
public abstract class QueuedEventCallback<TYPE extends BaseMessage, RTYPE extends BaseMessage> extends SimpleChannelHandler implements ResponseHandler {
  static Logger                              LOG          = Logger.getLogger( QueuedEventCallback.class );
  private Lock                               canHas       = new ReentrantLock( );
  private Condition                          ready        = canHas.newCondition( );
  private AtomicReference<Object>            response     = new AtomicReference<Object>( null );
  protected BlockingQueue<EucalyptusMessage> requestQueue = new LinkedBlockingQueue<EucalyptusMessage>( );
  
  public static class NOOP extends QueuedEventCallback {
    public NOOP() {
      RuntimeException ex = new RuntimeException( "Operation returning a NOOP." );
      LOG.debug( ex, ex );
      this.setRequest( new EucalyptusMessage() );
    }
    public void fail( Throwable throwable ) {}
    
    public void verify( BaseMessage msg ) throws Exception {}
    
    public void prepare( BaseMessage msg ) throws Exception {}
  }
  
  public QueuedEventCallback<TYPE, RTYPE> regarding( BaseMessage msg ) {
    this.getRequest( ).regarding( msg );
    return this;
  }
  
  public QueuedEventCallback<TYPE, RTYPE> regardingUserRequest( BaseMessage msg ) {
    this.getRequest( ).regardingUserRequest( msg );
    return this;
  }
  
  private AtomicReference<TYPE>        request         = new AtomicReference<TYPE>( null );
  private ChannelFuture                connectFuture;
  private NioBootstrap                 clientBootstrap;
  @SuppressWarnings( "unchecked" )
  private SuccessCallback              successCallback = SuccessCallback.NOOP;
  private FailureCallback<TYPE, RTYPE> failCallback    = FailureCallback.NOOP;
  
  public QueuedEventCallback<TYPE, RTYPE> then( UnconditionalCallback c ) {
    this.successCallback = c;
    this.failCallback = c;
    return this;
  }
  
  @SuppressWarnings( "unchecked" )
  public QueuedEventCallback<TYPE, RTYPE> then( SuccessCallback c ) {
    this.successCallback = c;
    return this;
  }
  
  public QueuedEventCallback<TYPE, RTYPE> then( FailureCallback<TYPE, RTYPE> c ) {
    this.failCallback = c;
    return this;
  }
  
  public void exceptionCaught( final ChannelHandlerContext ctx, final Throwable e ) {
    LOG.debug( e, e );
    this.queueResponse( e );
  }
  
  public void prepare( TYPE msg ) throws Exception {
    LOG.debug( this.getClass( ).getCanonicalName( ) + " should implement: prepare( TYPE t ) to check any preconditions!" );    
  }
  
  public abstract void verify( RTYPE msg ) throws Exception;
  
  public void fail( Throwable t ) {
    LOG.info( this.getClass( ).getCanonicalName( ) + " should implement: fail( Throwable t ) to handle errors!" );
    LOG.error( t, t );
  }
  
  public QueuedEventCallback dispatch( String clusterName ) {
    EventRecord.caller( QueuedEventCallback.class, EventType.QUEUE, this.getRequest( ).getClass( ), LogUtil.dumpObject( this.getRequest( ) ) ).debug( );
    Cluster cluster = Clusters.getInstance( ).lookup( clusterName );
    return this.dispatch( cluster );
  }
  
  public QueuedEventCallback dispatch( Cluster cluster ) {
    EventRecord.caller( QueuedEventCallback.class, EventType.QUEUE, this.getRequest( ).getClass( ), LogUtil.dumpObject( this.getRequest( ) ) ).debug( );
    cluster.getMessageQueue( ).enqueue( this );
    return this;
  }
  
  public RTYPE send( String clusterName ) throws Exception, Exception {
    return this.send( Clusters.getInstance( ).lookup( clusterName ) );
  }
  
  public RTYPE send( Cluster cluster ) throws Exception {
    this.fire( cluster.getHostName( ), cluster.getPort( ), cluster.getServicePath( ) );
    return this.getResponse( );
  }
  
  @SuppressWarnings( "unchecked" )
  @Override
  public void messageReceived( final ChannelHandlerContext ctx, final MessageEvent e ) throws Exception {
    if ( e.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse response = ( MappingHttpResponse ) e.getMessage( );
      try {
        RTYPE msg = ( RTYPE ) response.getMessage( );
        LOG.trace( msg.toString( ) );
        try {
          if ( !msg.get_return( ) ) {
            Exception ex = new EucalyptusClusterException( LogUtil.dumpObject( msg ) );
            this.fail( ex );
            this.failCallback.failure( this, ex );
            this.queueResponse( ex );
          } else {
            try {
              this.verify( msg );
              this.successCallback.apply( msg );
            } catch ( Throwable ex ) {
              LOG.error( ex , ex );
            }
            this.queueResponse( msg );
          }
        } catch ( Throwable e1 ) {
          LOG.error( e1, e1 );
          throw e1;
        }
      } catch ( Throwable e1 ) {
        try {
          this.fail( e1 );
        } catch ( Throwable e3 ) {
          LOG.error( e3, e3 );
        } finally {
          try {
            this.failCallback.failure( this, e1 );
          } catch ( Throwable e2 ) {
            LOG.debug( e2, e2 );
          }
        }
        this.queueResponse( e1 );
        e.getFuture( ).addListener( ChannelFutureListener.CLOSE );
        if ( e1 instanceof EucalyptusClusterException ) {
          throw ( EucalyptusClusterException ) e1;
        } else {
          throw new EucalyptusClusterException( "Error in contacting the Cluster Controller: " + e1.getMessage( ), e1 );
        }
      }
    }
    ctx.getChannel( ).close( );
  }
  
  public void queueResponse( Object o ) {
    EventRecord.here( this.getClass( ), EventType.MSG_REPLY, LogUtil.dumpObject( o ) );

    if ( o instanceof MappingHttpResponse ) {
      MappingHttpResponse httpResponse = ( MappingHttpResponse ) o;
      if ( httpResponse.getMessage( ) != null ) {
        o = httpResponse.getMessage( );
      } else {
        o = new EucalyptusClusterException( httpResponse.getMessageString( ) );
      }
    }
    this.canHas.lock( );
    try {
      if ( !this.response.compareAndSet( null, o ) ) {
        if ( !( o instanceof Throwable ) ) {
          LOG.debug( LogUtil.subheader( "Received spurious second response: " + LogUtil.dumpObject( o ) ) );
        }
        o = this.response.getAndSet( o );
        LOG.debug( LogUtil.subheader( "Previous response was: " + LogUtil.dumpObject( this.response.get( ) ) ) );
      } else {
        if ( o instanceof Throwable ) {
          LOG.error( "Caught exception in asynchronous response handler.", ( Throwable ) o );
        } else {
          LOG.debug( this.getClass( ).getSimpleName( ) + " Got response of: " + LogUtil.dumpObject( o ) );
        }
      }
      this.ready.signalAll( );
    } finally {
      this.canHas.unlock( );
    }
    EventRecord.here( this.getClass( ), EventType.MSG_REPLY, LogUtil.dumpObject( this.response.get( ) ) );
  }
  
  public Throwable getException( ) {
    this.waitForResponse( );
    return ( Throwable ) this.response.get( );
  }
  
  public TYPE getRequest( ) {
    return this.request.get( );
  }
  
  public boolean setRequest( TYPE request ) {
    return this.request.compareAndSet( null, request );
  }
  
  @Override
  public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) {
    try {
      this.fail( e.getCause( ) );
    } catch ( Throwable e1 ) {
      LOG.debug( e1, e1 );
    }
    this.exceptionCaught( ctx, e.getCause( ) );
    ctx.getChannel( ).close( );
  }
  
  public RTYPE pollResponse( Long waitMillis ) throws Exception {
    if ( !this.pollForResponse( waitMillis ) ) {
      return null;
    } else {
      EventRecord.here( NioResponseHandler.class, EventType.MSG_SERVICED, this.response.get( ).getClass( ).toString( ) ).debug( );
      return this.checkedResponse( );
    }
  }
  
  private RTYPE checkedResponse( ) throws Exception {
    if ( this.response.get( ) instanceof EucalyptusMessage ) {
      return ( RTYPE ) this.response.get( );
    } else if ( this.response.get( ) instanceof Throwable ) {
      throw new EucalyptusClusterException( "Exception in NIO request.", ( Throwable ) this.response.get( ) );
    }
    throw new EucalyptusClusterException( "Failed to retrieve result of asynchronous operation." );
  }
  
  @Override
  public RTYPE getResponse( ) throws Exception {
    this.waitForResponse( );
    EventRecord.here( NioResponseHandler.class, EventType.MSG_SERVICED, this.response.get( ).getClass( ).toString( ) ).debug( );
    return this.checkedResponse( );
  }
  
  public boolean pollForResponse( Long waitMillis ) {
    boolean ret = false;
    this.canHas.lock( );
    try {
      if (this.response.get( ) != null) {
	  return true;
      } else {
	  ret = this.ready.await( waitMillis, TimeUnit.MILLISECONDS );
	  EventRecord.here( NioResponseHandler.class, EventType.MSG_AWAIT_RESPONSE, EventType.MSG_POLL_INTERNAL.toString( ), waitMillis.toString( ) ).debug( );
      }
    } catch ( InterruptedException e ) {
      LOG.debug( e, e );
      Thread.currentThread( ).interrupt( );
    } finally {
      this.canHas.unlock( );
    }
    return ret;
  }
  
  public void waitForResponse( ) {
    this.canHas.lock( );
    try {
      while ( !this.pollForResponse( 100l ) );
    } finally {
      this.canHas.unlock( );
    }
  }
  
  public void fire( final String hostname, final int port, final String servicePath ) {
    try {
      NioClientPipeline clientPipeline = new ClusterClientPipeline( this );
      this.clientBootstrap = ChannelUtil.getClientBootstrap( clientPipeline );
      InetSocketAddress addr = new InetSocketAddress( hostname, port );
      this.connectFuture = this.clientBootstrap.connect( addr );
      HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, addr.getHostName( ), addr.getPort( ), servicePath, this.getRequest( ) );
      this.prepare( this.getRequest( ) );
      this.connectFuture.addListener( ChannelUtil.WRITE( request ) );
    } catch ( Throwable e ) {
      try {
        this.fail( e );
        this.failCallback.failure( this, e );
      } catch ( Throwable e1 ) {
        LOG.debug( e1, e1 );
      }
      this.queueResponse( e );
      this.connectFuture.addListener( ChannelFutureListener.CLOSE );
    }
  }
  
  @Override
  public void channelClosed( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
    if ( !this.writeComplete ) {
      this.queueResponse( new EucalyptusClusterException( "Channel was closed before the write operation could be completed: "
                                                          + LogUtil.dumpObject( this.getRequest( ) ) ) );
    } else if ( writeComplete && this.getRequest( ) == null ) {
      this.queueResponse( new EucalyptusClusterException( "Channel was closed before the read operation could be completed: "
                                                          + LogUtil.dumpObject( this.getRequest( ) ) ) );
    }
    super.channelClosed( ctx, e );
  }
  
  private volatile boolean writeComplete = false;
  
  @Override
  public void writeComplete( ChannelHandlerContext ctx, WriteCompletionEvent e ) throws Exception {
    super.writeComplete( ctx, e );
    this.writeComplete = true;
  }
  
}
