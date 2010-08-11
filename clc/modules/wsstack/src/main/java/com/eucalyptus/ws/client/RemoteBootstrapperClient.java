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
package com.eucalyptus.ws.client;

import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.event.LifecycleEvent;
import com.eucalyptus.component.event.StartComponentEvent;
import com.eucalyptus.component.event.StopComponentEvent;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.NetworkUtil;
import com.eucalyptus.ws.handlers.BindingHandler;
import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.eucalyptus.ws.handlers.soap.AddressingHandler;
import com.eucalyptus.ws.handlers.soap.SoapHandler;
import com.eucalyptus.ws.handlers.wssecurity.InternalWsSecHandler;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

@Provides(Component.eucalyptus)
@RunDuring(Bootstrap.Stage.RemoteConfiguration)
@DependsLocal(Component.eucalyptus)
public class RemoteBootstrapperClient extends Bootstrapper implements ChannelPipelineFactory, EventListener {
  private static Logger                            LOG    = Logger.getLogger( RemoteBootstrapperClient.class );
  private ConcurrentMap<String, HeartbeatClient>   heartbeatMap;
  private Multimap<String, ServiceConfiguration> componentMap;
  private NioBootstrap                             clientBootstrap;
  private ChannelFactory                           channelFactory;
  private static RemoteBootstrapperClient          client = new RemoteBootstrapperClient( );

  public static RemoteBootstrapperClient getInstance( ) {
    return client;
  }

  private RemoteBootstrapperClient( ) {
    this.channelFactory = new NioClientSocketChannelFactory( Executors.newCachedThreadPool( ), Executors.newCachedThreadPool( ) );
    this.clientBootstrap = new NioBootstrap( channelFactory );
    this.clientBootstrap.setPipelineFactory( this );
    this.componentMap = Multimaps.newArrayListMultimap( );
    this.heartbeatMap = Maps.newConcurrentHashMap( );
  }

  public ChannelPipeline getPipeline( ) throws Exception {
    ChannelPipeline pipeline = Channels.pipeline( );
    pipeline.addLast( "decoder", new HttpRequestEncoder( ) );
    pipeline.addLast( "encoder", new HttpResponseDecoder( ) );
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
    pipeline.addLast( "handler", new HeartbeatHandler( ) );
    return pipeline;
  }

  @ChannelPipelineCoverage( "one" )
  class HeartbeatHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {

    @Override
    public void handleDownstream( ChannelHandlerContext ctx, ChannelEvent e ) {
      if ( e instanceof ExceptionEvent ) {
        LOG.error( ( ( ExceptionEvent ) e ).getCause( ), ( ( ExceptionEvent ) e ).getCause( ) );
        ctx.getChannel( ).close( );
      } else {
        ctx.sendDownstream( e );
      }
    }

    @Override
    public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) {
      if ( e instanceof ExceptionEvent ) {
        LOG.error( ( ( ExceptionEvent ) e ).getCause( ), ( ( ExceptionEvent ) e ).getCause( ) );
        ctx.getChannel( ).close( );
      } else {
        ctx.sendUpstream( e );
      }
    }

  }

  @Override
  public boolean load( Stage current ) throws Exception {
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    List<ServiceConfiguration> configs = Lists.newArrayList( );
    configs.addAll( Configuration.getStorageControllerConfigurations( ) );
    configs.addAll( Configuration.getWalrusConfigurations( ) );
    for( ServiceConfiguration c : configs ) {
      if( !c.isLocal( ) ) {
        this.addRemoteComponent( c );
        this.fireRemoteStartEvent( c );
      }
    }
    return true;
  }

  @Override
  public void advertiseEvent( Event event ) {}

  @Override
  public void fireEvent( Event event ) {
    if ( event instanceof LifecycleEvent ) {
      LifecycleEvent e = ( LifecycleEvent ) event;
      if ( !Component.walrus.equals( e.getPeer( ) ) && !Component.storage.equals( e.getPeer( ) ) ) {
        return;
      } else if ( e.getConfiguration( ).getPort( ) < 0 ) {
        return;
      } else {
        ServiceConfiguration config = e.getConfiguration( );
        if ( event instanceof StartComponentEvent ) {
          if( !NetworkUtil.testLocal( e.getConfiguration( ).getHostName( ) ) ) {
            this.addRemoteComponent( config );
          }
          this.fireRemoteStartEvent( config );
        } else if ( event instanceof StopComponentEvent ) {
          if( !NetworkUtil.testLocal( e.getConfiguration( ).getHostName( ) ) ) {
            this.removeRemoteComponent( config );
          }
          this.fireRemoteStopEvent( config );
        }
      }
    } else if ( event instanceof ClockTick ) {
      if ( ( ( ClockTick ) event ).isBackEdge( ) ) {
        this.fireHeartbeat( );
      }
    }
  }

  private void fireHeartbeat( ) {
    for ( HeartbeatClient hb : this.heartbeatMap.values( ) ) {
      Collection<ServiceConfiguration> services = this.componentMap.get( hb.getHostName( ) );
      LOG.debug( "Sending heartbeat to: " + hb.getHostName( ) + " with " + services ); 
      hb.send( services );
    }
  }

  private void fireRemoteStartEvent( ServiceConfiguration config ) {
    for ( HeartbeatClient hb : this.heartbeatMap.values( ) ) {
      if ( hb.getHostName( ).equals( config.getHostName( ) ) ) {
        LOG.info( "--> Firing start event on target remote component: " + LogUtil.dumpObject( hb ) );
        hb.send( this.componentMap.get( hb.getHostName( ) ) );
      } else {
        LOG.info( "--> Queueing start event on all other remote components: " + LogUtil.dumpObject( hb ) );
        hb.addStarted( config );
        this.fireHeartbeat( );
      }
    }
  }

  private void fireRemoteStopEvent( ServiceConfiguration config ) {
    for ( HeartbeatClient hb : this.heartbeatMap.values( ) ) {
      if ( hb.getHostName( ).equals( config.getHostName( ) ) ) {
        LOG.info( "--> Firing stop event on target remote component: " + LogUtil.dumpObject( hb ) );
        hb.send( this.componentMap.get( hb.getHostName( ) ) );
      } else {
        LOG.info( "--> Queueing start event for next clock tick on all other remote components: " + LogUtil.dumpObject( hb ) );
        hb.addStopped( config );
        this.fireHeartbeat( );
      }
    }
  }

  public void addRemoteComponent( ServiceConfiguration config ) {
    if ( !this.heartbeatMap.containsKey( config.getHostName( ) ) ) {
      LOG.debug( LogUtil.subheader( "-> Adding remote bootstrapper for host: " + config.getHostName( ) ) );
      this.heartbeatMap.put( config.getHostName( ), new HeartbeatClient( this.clientBootstrap, config.getHostName( ), config.getPort( ) ) );
    }
    if ( !this.componentMap.containsEntry( config.getHostName(), config ) ) {
      LOG.debug( "-> Adding remote component to the bootstrapper map: " + LogUtil.dumpObject( config ) );
      this.componentMap.put( config.getHostName( ), config );
    }
  }

  public void removeRemoteComponent( ServiceConfiguration config ) {
    if ( this.componentMap.containsEntry( config.getHostName( ), config ) ) {
      LOG.debug( "-> Removing remote component bootstrapper: " + LogUtil.dumpObject( config ) );
      this.componentMap.remove( config.getHostName( ), config );
      if ( this.componentMap.get( config.getHostName( ) ).isEmpty( ) ) {
        HeartbeatClient hb = this.heartbeatMap.remove( config.getHostName( ) );
        hb.send( this.componentMap.get( hb.getHostName( ) ) );
        hb.close( );
        LOG.debug( LogUtil.subheader( "-> Removing remote bootstrapper for host: " + config.getHostName( ) ) );
      }
    }
  }

}
