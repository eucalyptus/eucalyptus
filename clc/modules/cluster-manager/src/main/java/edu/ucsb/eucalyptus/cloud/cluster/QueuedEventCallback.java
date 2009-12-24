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
package edu.ucsb.eucalyptus.cloud.cluster;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.FailureCallback;
import com.eucalyptus.cluster.SuccessCallback;
import com.eucalyptus.cluster.UnconditionalCallback;
import com.eucalyptus.util.EucalyptusClusterException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.MappingHttpResponse;
import com.eucalyptus.ws.client.NioBootstrap;
import com.eucalyptus.ws.client.pipeline.ClusterClientPipeline;
import com.eucalyptus.ws.client.pipeline.NioClientPipeline;
import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.util.ChannelUtil;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.EventRecord;

@SuppressWarnings( "unchecked" )
public abstract class QueuedEventCallback<TYPE> extends NioResponseHandler {//FIXME: the generic here conflicts with a general use for queued event.
  static Logger                 LOG     = Logger.getLogger( QueuedEventCallback.class );
  public static class NOOP extends QueuedEventCallback {
    public void fail( Throwable throwable ) {}
    public void prepare( Object msg ) throws Exception {}
    public void verify( EucalyptusMessage msg ) throws Exception {}
  }
  private AtomicReference<TYPE> request = new AtomicReference<TYPE>( null );
  private ChannelFuture         connectFuture;
  private NioBootstrap          clientBootstrap;
  @SuppressWarnings( "unchecked" )
  private SuccessCallback       successCallback = SuccessCallback.NOOP;
  private FailureCallback<TYPE> failCallback = FailureCallback.NOOP;
  
  public QueuedEventCallback<TYPE> then( UnconditionalCallback c ) {
    this.successCallback = c;
    this.failCallback = c;
    return this;
  }
  @SuppressWarnings( "unchecked" )
  public QueuedEventCallback<TYPE> then( SuccessCallback c ) {
    this.successCallback = c;
    return this;
  }
  public QueuedEventCallback<TYPE> then( FailureCallback<TYPE> c ) {
    this.failCallback = c;
    return this;
  }
  
  public void dispatch( String clusterName ) {
    LOG.debug( EventRecord.caller(QueuedEventCallback.class,this.getRequest().getClass(),LogUtil.dumpObject( this.getRequest() )));
    dispatch( Clusters.getInstance( ).lookup( clusterName ) );
  }
  public void dispatch( Cluster cluster ) {
    LOG.debug( EventRecord.caller(QueuedEventCallback.class,this.getRequest().getClass(),LogUtil.dumpObject( this.getRequest() )));
    cluster.getMessageQueue( ).enqueue( QueuedEvent.make( this, this.getRequest( ) ) );
  }
  
  public void send( String clusterName ) {
    send( Clusters.getInstance( ).lookup( clusterName ) );
  }
  public void send( Cluster cluster ) {
    this.fire( cluster.getHostName( ), cluster.getPort( ), cluster.getServicePath( ), this.getRequest( ) );
  }
  
  @SuppressWarnings( "unchecked" )
  @Override
  public void messageReceived( final ChannelHandlerContext ctx, final MessageEvent e ) throws Exception {
    if ( e.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse response = ( MappingHttpResponse ) e.getMessage( );
      try {
        EucalyptusMessage msg = ( EucalyptusMessage ) response.getMessage( );
        if( !msg.get_return( ) ) {
          throw new EucalyptusClusterException( LogUtil.dumpObject( msg ) );
        }
        this.verify( msg );
        try {
          this.successCallback.apply( msg );
        } catch ( Throwable e1 ) {
          LOG.debug( e1, e1 );
          this.failCallback.failure( this, e1 );
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
        super.queueResponse( e1 );
        e.getFuture( ).addListener( ChannelFutureListener.CLOSE );
        if( e1 instanceof EucalyptusClusterException ) {
          throw (EucalyptusClusterException)e1;
        } else {
          throw new EucalyptusClusterException( "Error in contacting the Cluster Controller: " + e1.getMessage( ), e1 );
        }
      }
    }
    super.messageReceived( ctx, e );
  }
  
  public abstract void prepare( TYPE msg ) throws Exception;
  
  public abstract void verify( EucalyptusMessage msg ) throws Exception;
  
  public abstract void fail( Throwable throwable );
  
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
    super.exceptionCaught( ctx, e );
  }
  
  public void fire( final String hostname, final int port, final String servicePath, final TYPE msg ) {
    try {
      NioClientPipeline clientPipeline = new ClusterClientPipeline( this );
      this.clientBootstrap = ChannelUtil.getClientBootstrap( clientPipeline );
      InetSocketAddress addr = new InetSocketAddress( hostname, port );
      this.request.set( msg );
      this.connectFuture = this.clientBootstrap.connect( addr );
      HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, addr.getHostName( ), addr.getPort( ), servicePath, msg );
      this.prepare( msg );
      this.connectFuture.addListener( ChannelUtil.WRITE( request ) );
    } catch ( Throwable e ) {
      try {
        this.fail( e );
      } catch ( Exception e1 ) {
        LOG.debug( e1, e1 );
      }
      this.failCallback.failure( this, e );
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
