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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.handlers.AbstractClusterMessageDispatcher;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.MappingHttpResponse;
import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public abstract class QueuedEventCallback<TYPE> extends NioResponseHandler {//FIXME: the generic here conflicts with a general use for queued event.
  private static Logger LOG = Logger.getLogger( QueuedEventCallback.class );
  private ChannelFuture channelConnected;
  private TYPE          request;
  private ChannelFutureListener CLEAR_PENDING = new ChannelFutureListener( ) {
    @Override
    public void operationComplete( ChannelFuture future ) throws Exception {
      QueuedEventCallback.this.inFlightMessage.lazySet( false );                
    }
  };  
  private AtomicBoolean inFlightMessage = new AtomicBoolean( false );

  public void process( TYPE msg ) throws Exception {
    super.getRequestQueue( ).add( ( EucalyptusMessage ) msg );
    if ( this.channelConnected != null && this.channelConnected.isDone( ) && this.inFlightMessage.compareAndSet( false, true ) ) {
      this.channelConnected = null;
      this.fireMessage( msg, this.channelConnected.getChannel( ) );
    } else {
      LOG.debug( "Found channel pending: defering message " + msg.getClass( ).getCanonicalName( ) );
    }
  }
  
  private void fireMessage( TYPE msg, Channel channel ) throws Exception {
    LOG.debug( "Found channel open: writing message " + msg.getClass( ).getCanonicalName( ) );
    String servicePath = "/axis2/services/EucalyptusCC";//FIXME: handle this in a clean way.
    InetSocketAddress addr = ( InetSocketAddress ) channel.getRemoteAddress( );
    this.setRequest( msg );
    HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, addr.getHostName( ), addr.getPort( ), servicePath, msg );
    try {
      this.prepare( msg );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      channel.close( ).addListener( CLEAR_PENDING );
      throw e;
    }
    channel.write( request );      
  }
  
  public abstract void prepare( TYPE msg ) throws Exception;
  
  public abstract void verify( EucalyptusMessage msg ) throws Exception;
  
  @Override
  public void messageReceived( final ChannelHandlerContext ctx, final MessageEvent e ) throws Exception {
    if ( e.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse response = (MappingHttpResponse) e.getMessage( );
      try {
        this.verify( (EucalyptusMessage)response.getMessage( ) );
      } catch ( Throwable e1 ) {
        LOG.debug( e1, e1 );
        e.getChannel( ).close( );
        throw new RuntimeException( e1 );
      }
    }
    super.messageReceived( ctx, e );
  }
  
  @Override
  public void channelConnected( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
    this.channelConnected = e.getFuture( );
    ctx.getChannel( ).getCloseFuture( ).addListener( CLEAR_PENDING );
    if ( super.getRequestQueue( ).isEmpty( ) ) {
      LOG.debug( "Request queue is empty, waiting for write request." );
      ctx.getChannel( ).close( );
    } else if ( this.inFlightMessage.compareAndSet( false, true )  ) {
      Object msg = this.getRequestQueue( ).peek( );
      LOG.debug( "Found pending request in the request queue: " + msg.getClass( ).getCanonicalName( ) );
      this.fireMessage( ( TYPE ) msg, e.getChannel( ) );
    }
    super.channelConnected( ctx, e );
  }
  
  @Override
  public void channelOpen( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
    super.channelOpen( ctx, e );
  }
  
  public abstract static class MultiClusterCallback<TYPE extends EucalyptusMessage> extends QueuedEventCallback<TYPE> {
    private List<QueuedEvent> callbackList = Lists.newArrayList( );
    public abstract MultiClusterCallback<TYPE> newInstance();
    public abstract void prepareAll( TYPE msg ) throws Exception;
    
    protected List<QueuedEvent> fireEventAsyncToAllClusters( final TYPE msg ) {
      this.callbackList = Lists.newArrayList( );
      for ( final Cluster c : Clusters.getInstance( ).listValues( ) ) {
        LOG.info( "-> Sending " + msg.getClass( ).getSimpleName( ) + " network to: " + c.getUri( ) );
        LOG.debug( LogUtil.lineObject( msg ) );
        try {
          QueuedEvent q = QueuedEvent.make( this.newInstance( ), msg );
          callbackList.add( q );
          Clusters.sendClusterEvent( c, q );
        } catch ( final Throwable e ) {
          LOG.error( "Error while sending to: " + c.getUri( ) + " " + msg.getClass( ).getSimpleName( ) );
        }
      }
      return callbackList;
    }
        
  }
  
  public TYPE getRequest( ) {
    return this.request;
  }
  
  public void setRequest( TYPE request ) {
    this.request = request;
  }

}
