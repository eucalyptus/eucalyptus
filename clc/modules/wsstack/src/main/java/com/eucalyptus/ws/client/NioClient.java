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
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.client;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.client.pipeline.NioClientPipeline;
import com.eucalyptus.ws.handlers.MessageStackHandler;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class NioClient implements Client {
  private static Logger LOG = Logger.getLogger( NioClient.class );

  private NioBootstrap clientBootstrap;

  private ChannelFactory channelFactory;
  private ChannelFuture channelOpenFuture;
  private ChannelFuture channelWriteFuture;
  private Channel channel;
  private NioClientPipeline clientPipeline;


  String hostname;
  int port;
  String servicePath;
  InetSocketAddress remoteAddr;

  public NioClient( String hostname, int port, String servicePath, NioClientPipeline clientPipeline ) {
    this.channelFactory = new NioClientSocketChannelFactory( Executors.newCachedThreadPool(), Executors.newCachedThreadPool() );
    this.clientBootstrap = new NioBootstrap( channelFactory );
    this.clientBootstrap.setPipelineFactory( clientPipeline );
    this.clientPipeline = clientPipeline;
    this.remoteAddr = new InetSocketAddress( hostname, port );
    this.hostname = hostname;
    this.port = port;
    this.servicePath = servicePath;
  }

  public void write( HttpRequest httpRequest ) throws Exception {
    if ( this.channel == null || !this.channel.isOpen() || !this.channel.isConnected() ) {
      this.channelOpenFuture = this.clientBootstrap.connect( this.remoteAddr );
      this.channelOpenFuture.addListener( new DeferedWriter( httpRequest, this.clientPipeline.getHandler() ) );
    }
  }

  class DeferedWriter implements ChannelFutureListener {
    private HttpRequest httpRequest;
    private MessageStackHandler handler;

    DeferedWriter( final HttpRequest httpRequest, final MessageStackHandler handler ) {
      this.httpRequest = httpRequest;
      this.handler = handler;
    }

    @Override
    public void operationComplete( final ChannelFuture channelFuture ) throws Exception {
      if( channelFuture.isSuccess() ) {
        channel = channelFuture.getChannel();
        channelWriteFuture = channelFuture.getChannel().write( this.httpRequest );
      } else {
        this.handler.exceptionCaught( channelFuture.getCause() );
      }
    }
  }

  public void close() {
    if ( this.channelWriteFuture != null && !this.channelWriteFuture.isDone() ) {
      this.channelWriteFuture.awaitUninterruptibly();
    }
    this.channel.close();
    LOG.debug( "Forcing the channel to close." );
  }

  public void cleanup() {
    if ( this.channel != null ) { this.close(); }
    this.channelFactory.releaseExternalResources();
  }

  @Override
  public EucalyptusMessage send( final EucalyptusMessage msg ) throws Exception {
    HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, this.hostname, this.port, this.servicePath, msg );
    this.write( request );
    EucalyptusMessage response = this.clientPipeline.getHandler().getResponse();
    this.cleanup();
    return response;
  }

  @Override
  public void dispatch( final EucalyptusMessage msg ) throws Exception {
    HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, this.hostname, this.port, this.servicePath, msg );
    this.write( request );
    EucalyptusMessage response = this.clientPipeline.getHandler().getResponse();
    this.cleanup();
  }

  @Override
  public String getUri() {
    return "http://"+this.hostname+":"+this.port+(servicePath.startsWith( "/" )?"":"?")+servicePath;
  }
}
