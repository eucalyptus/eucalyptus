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

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.NetworkUtil;
import com.eucalyptus.ws.RemoteConfigurationException;
import com.eucalyptus.ws.client.pipeline.NioClientPipeline;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.ws.handlers.NioHttpResponseDecoder;
import com.eucalyptus.ws.handlers.http.NioHttpRequestEncoder;

import edu.ucsb.eucalyptus.StartupChecks;

@Provides(resource=Resource.RemoteConfiguration)
@Depends(resources=Resource.Database,local=Component.eucalyptus)
public class RemoteBootstrapperClient extends Bootstrapper implements Runnable, ChannelPipelineFactory {
  private static Logger     LOG = Logger.getLogger( RemoteBootstrapperClient.class );

  private NioBootstrap      clientBootstrap;
  private ChannelFactory    channelFactory;
  private NioClientPipeline clientPipeline;
  private static boolean hack = false;
  public RemoteBootstrapperClient( ) {
    this.channelFactory = new NioClientSocketChannelFactory( Executors.newCachedThreadPool( ), Executors.newCachedThreadPool( ) );
    this.clientBootstrap = new NioBootstrap( channelFactory );
    this.clientBootstrap.setPipelineFactory( this );
  }

  public ChannelPipeline getPipeline( ) throws Exception {
    ChannelPipeline pipeline = Channels.pipeline( );
    pipeline.addLast( "decoder", new HttpResponseDecoder( ) );
    pipeline.addLast( "encoder", new HttpRequestEncoder( ) );
    pipeline.addLast( "heartbeat", new HeartbeatHandler( ) );
    return pipeline;
  }
  
  @ChannelPipelineCoverage("one")
  class HeartbeatHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {

    @Override
    public void handleDownstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      if( e instanceof ExceptionEvent ) {
        ctx.getChannel( ).close( );
      } else {
        ctx.sendDownstream( e );        
      }
    }

    @Override
    public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      if( e instanceof ExceptionEvent ) {
        ctx.getChannel( ).close( );
      } else {
        ctx.sendUpstream( e );
      }
    }
    
  }

  class HeartbeatClient {
    private String            hostname;
    private int               port;
    private String            servicePath;
    private InetSocketAddress remoteAddr;
    private ChannelFuture     channelOpenFuture;
    private ChannelFuture     channelWriteFuture;
    private Channel           channel;

    public HeartbeatClient( String hostname, int port, String servicePath ) {
      this.remoteAddr = new InetSocketAddress( hostname, port );
      this.servicePath = servicePath;
    }

    public void write( final HttpRequest httpRequest ) throws Exception {
      if ( this.channel == null || !this.channel.isOpen( ) || !this.channel.isConnected( ) ) {
        this.channelOpenFuture = clientBootstrap.connect( this.remoteAddr );
        this.channelOpenFuture.addListener( new DeferedWriter( httpRequest ) ); 
      } else {
        channelWriteFuture = this.channel.write( httpRequest );
        channelWriteFuture.addListener( ChannelFutureListener.CLOSE );
      }
    }
    class DeferedWriter implements ChannelFutureListener {
      private HttpRequest httpRequest;
      public DeferedWriter( HttpRequest httpRequest ) {
        this.httpRequest = httpRequest;
      }
      @Override
      public void operationComplete( ChannelFuture channelFuture ) throws Exception {
        if ( channelFuture.isSuccess( ) ) {
          channel = channelFuture.getChannel( );
          channelWriteFuture = channelFuture.getChannel( ).write( httpRequest );
          channelWriteFuture.addListener( ChannelFutureListener.CLOSE );
          LOG.info( "Sending configuration info to: " + channel.getRemoteAddress( ) );
        } else {
          if( channelFuture != null ) {
            LOG.warn( "Failed to connect to heartbeat service at " + remoteAddr + ": " + channelFuture.getCause( ).getMessage( ) );
          }
          if( channel != null ) {
            channel.close( );            
          }
        }
      }
    }
  }


  @Override
  public boolean load( Resource current ) throws Exception {
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    (new Thread( new RemoteBootstrapperClient())).start();
    if( !hack ) {
      StartupChecks.createDb( );
      hack = true;
    }
    return true;
  }

  @Override
  public void run( ) {
    while(true){
      try {
        List<WalrusConfiguration> walrusConfigs = Configuration.getWalrusConfigurations( );
        for( ComponentConfiguration w : walrusConfigs ) {
          String configuration = this.getConfigurationBuffer( w );
          ChannelBuffer buffer = ChannelBuffers.copiedBuffer( configuration.getBytes( ) );
          this.sendConfiguration( w, buffer );
        }
      } catch ( Exception e1 ) {
        LOG.warn( "Error configuring remote walrus." );
        LOG.error( e1,e1 );
      }
      try {
        List<StorageControllerConfiguration> scConfigs = Configuration.getStorageControllerConfigurations( );
        for( ComponentConfiguration sc : scConfigs ) {
          String configuration = this.getConfigurationBuffer( sc );
          ChannelBuffer buffer = ChannelBuffers.copiedBuffer( configuration.getBytes( ) );
          this.sendConfiguration( sc, buffer );
        }
      } catch ( Exception e1 ) {
        LOG.warn( "Error configuring remote storage controller." );
        LOG.error( e1,e1 );
      }
      try {
        Thread.sleep(5000);
      } catch ( InterruptedException e ) {}
    }
  }

  private void sendConfiguration( ComponentConfiguration w, ChannelBuffer buffer  ) throws Exception {
    HeartbeatClient hb = new HeartbeatClient( w.getHostName( ), 8773, "/services/Heartbeat" );
    HttpRequest httpRequest = new DefaultHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, "/services/Heartbeat" );
    httpRequest.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
    httpRequest.addHeader( HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=UTF-8" );
    httpRequest.setContent( buffer );
    hb.write( httpRequest );
  }
  
  private String getConfigurationBuffer( ComponentConfiguration componentConfig ) throws Exception {
    List<String> possibleAddresses = NetworkUtil.getAllAddresses( );
    String configuration =  "euca.db.password=\neuca.db.port=9001\n";
    configuration += String.format( "euca.%s.name=%s\n", componentConfig.getClass( ).getSimpleName( ).replaceAll( "Configuration", "" ).toLowerCase( ), componentConfig.getName( ) );
    int i = 0;
    for( String s : possibleAddresses ) {
      configuration += String.format("euca.db.host.%d=%s\n",i++,s);
    }
    if( i == 0 ) {
      throw new RemoteConfigurationException("Failed to determine candidate database addresses.");//this should never happen
    }
    return configuration;
  }

}
