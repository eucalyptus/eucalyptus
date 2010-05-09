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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.client;

import java.net.SocketAddress;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineException;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.eucalyptus.bootstrap.Component;

import com.eucalyptus.records.EventRecord;

public class NioBootstrap extends Bootstrap {
  
  @ChannelPipelineCoverage( "one" )
  static final class Connector extends SimpleChannelUpstreamHandler {
    private final Bootstrap                    bootstrap;
    private final SocketAddress                localAddress;
    private final BlockingQueue<ChannelFuture> futureQueue;
    private final SocketAddress                remoteAddress;
    private volatile boolean                   finished = false;
    
    Connector( final Bootstrap bootstrap, final SocketAddress remoteAddress, final SocketAddress localAddress,
               final BlockingQueue<ChannelFuture> futureQueue ) {
      this.bootstrap = bootstrap;
      this.localAddress = localAddress;
      this.futureQueue = futureQueue;
      this.remoteAddress = remoteAddress;
    }
    
    @Override
    public void channelBound( final ChannelHandlerContext context, final ChannelStateEvent event ) {
      context.sendUpstream( event );
      if ( this.localAddress != null ) {
        this.finished = this.futureQueue.offer( event.getChannel( ).connect( this.remoteAddress ) );
      }
    }
    
    @Override
    public void channelOpen( final ChannelHandlerContext context, final ChannelStateEvent event ) {
      try {
        event.getChannel( ).getConfig( ).setOptions( this.bootstrap.getOptions( ) );
        event.getChannel( ).getConfig( ).setConnectTimeoutMillis( 3000 );
        event.getChannel( ).getConfig( ).setOption( "tcpNoDelay", true );
        event.getChannel( ).getConfig( ).setOption( "keepAlive", true );
        event.getChannel( ).getConfig( ).setOption( "reuseAddress", true );
        event.getChannel( ).getConfig( ).setOption( "connectTimeoutMillis", 3000 );
      } finally {
        context.sendUpstream( event );
      }
      
      // Bind or connect.
      if ( this.localAddress != null ) {
        event.getChannel( ).bind( this.localAddress );
      } else {
        this.finished = this.futureQueue.offer( event.getChannel( ).connect( this.remoteAddress ) );
        assert this.finished;
      }
    }
    
    @Override
    public void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent e ) throws Exception {
      ctx.sendUpstream( e );
      
      final Throwable cause = e.getCause( );
      if ( !( cause instanceof NotYetConnectedException ) && !this.finished ) {
        e.getChannel( ).close( );
        this.finished = this.futureQueue.offer( Channels.failedFuture( e.getChannel( ), cause ) );
        assert this.finished;
      }
    }
  }
  
  public NioBootstrap( ) {
    super( );
  }
  
  public NioBootstrap( final ChannelFactory channelFactory ) {
    super( channelFactory );
  }
  
  public ChannelFuture connect( final SocketAddress remoteAddress ) {
    if ( remoteAddress == null ) {
      throw new NullPointerException( "remotedAddress" );
    }
    final SocketAddress localAddress = ( SocketAddress ) this.getOption( "localAddress" );
    return this.connect( remoteAddress, localAddress );
  }
  
  private static Logger LOG = Logger.getLogger( NioBootstrap.class );
  
  public ChannelFuture connect( final SocketAddress remoteAddress, final SocketAddress localAddress ) {
    
    if ( remoteAddress == null ) {
      throw new NullPointerException( "remoteAddress" );
    }
    
    final BlockingQueue<ChannelFuture> futureQueue = new LinkedBlockingQueue<ChannelFuture>( );
    ChannelPipeline pipeline;
    try {
      pipeline = this.getPipelineFactory( ).getPipeline( );
    } catch ( final Exception e ) {
      throw new ChannelPipelineException( "Failed to initialize a pipeline.", e );
    }
    
    pipeline.addFirst( "connector", new Connector( this, remoteAddress, localAddress, futureQueue ) );
    this.getFactory( ).newChannel( pipeline );
    ChannelFuture future = null;
    do {
      try {
        future = futureQueue.poll( 50, TimeUnit.MILLISECONDS );
      } catch ( final InterruptedException e ) {}
    } while ( future == null );
    pipeline.remove( "connector" );

    return future;
  }
}
