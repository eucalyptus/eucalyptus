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
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.server;

import static org.jboss.netty.channel.Channels.pipeline;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.DependsRemote;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Service;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.ws.handlers.BindingHandler;
import com.eucalyptus.ws.handlers.HeartbeatHandler;
import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.eucalyptus.ws.handlers.http.NioHttpDecoder;
import com.eucalyptus.ws.handlers.soap.AddressingHandler;
import com.eucalyptus.ws.handlers.soap.SoapHandler;
import com.eucalyptus.ws.handlers.wssecurity.InternalWsSecHandler;
import com.eucalyptus.ws.util.ChannelUtil;

@Provides(Component.eucalyptus)
@RunDuring(Bootstrap.Stage.RemoteConfiguration)
@DependsRemote(Component.eucalyptus)
@ChannelPipelineCoverage( "all" )
public class RemoteBootstrapperServer extends Bootstrapper implements ChannelPipelineFactory {
  private static Logger                   LOG = Logger.getLogger( RemoteBootstrapperServer.class );
  private int                             port;
  private ServerBootstrap                 bootstrap;
  private Channel                         channel;
  private static RemoteBootstrapperServer server;
  
  public static RemoteBootstrapperServer getServer( ) {
    return server;
  }
  
  public RemoteBootstrapperServer( ) {
    this.port = ChannelUtil.PORT;
    ChannelUtil.setupServer( );
    this.bootstrap = ChannelUtil.getServerBootstrap( );
    this.bootstrap.setPipelineFactory( this );
  }
  
  @Override
  public boolean load( Stage current ) throws Exception {
    this.channel = this.bootstrap.bind( new InetSocketAddress( this.port ) );
    LOG.info( "Waiting for system properties before continuing bootstrap." );
    this.channel.getCloseFuture( ).awaitUninterruptibly( );
    LOG.info( "Channel closed, proceeding with bootstrap." );
    return true;
  }

  public boolean start( ) {
    return true;
  }
  
  @Provides(Component.bootstrap)
  @RunDuring(Bootstrap.Stage.RemoteServicesInit)
  @DependsRemote(Component.eucalyptus)
  public static class DeferedRemoteServiceBootstrapper extends Bootstrapper {
    @Override
    public boolean start( ) throws Exception {
      for( com.eucalyptus.component.Component c : Components.list( ) ) {
        for( Service s : c.getServices( ) ) {
          if( s.isLocal( ) ) {
            try {
              c.startService( s.getServiceConfiguration( ) );
            } catch ( ServiceRegistrationException ex ) {
              LOG.error( ex , ex );
              System.exit( 123 );
            }
          }
        }
      }
      return true;
    }

    @Override
    public boolean load( Stage current ) throws Exception {
      return true;
    }
    
  }
  
  public ChannelPipeline getPipeline( ) throws Exception {
    ChannelPipeline pipeline = pipeline( );
    pipeline.addLast( "decoder", new NioHttpDecoder( ) );
    pipeline.addLast( "encoder", new HttpResponseEncoder( ) );
    pipeline.addLast( "chunkedWriter", new ChunkedWriteHandler( ) );
    pipeline.addLast( "deserialize", new SoapMarshallingHandler( ) );
    try {
      pipeline.addLast( "ws-security", new InternalWsSecHandler( ) );
    } catch ( GeneralSecurityException e ) {
      LOG.error( e, e );
    }
    pipeline.addLast( "ws-addressing", new AddressingHandler( ) );
    pipeline.addLast( "build-soap-envelope", new SoapHandler( ) );
    pipeline.addLast( "binding", new BindingHandler( BindingManager.getBinding( "msgs_eucalyptus_com" ) ) );
    pipeline.addLast( "handler", new HeartbeatHandler( this.channel ) );
    return pipeline;
  }
  
  public Channel getChannel( ) {
    return channel;
  }
  
}
