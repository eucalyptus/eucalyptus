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

package com.eucalyptus.cluster;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.Handlers;
import com.eucalyptus.ws.StackConfiguration;
import com.eucalyptus.ws.handlers.ClusterWsSecHandler;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@ComponentPart( ClusterController.class )
public final class ClusterClientPipelineFactory implements ChannelPipelineFactory {
  private static Logger LOG = Logger.getLogger( ClusterClientPipelineFactory.class );
  private enum ClusterWsSec implements Supplier<ChannelHandler> {
    INSTANCE;
    
    @Override
    public ChannelHandler get( ) {
      return new ClusterWsSecHandler( );

    }
  };

  private static final Supplier<ChannelHandler> wsSecHandler = Suppliers.memoize( ClusterWsSec.INSTANCE );
  public static final Supplier<Integer>                     CLUSTER_CLIENT_PERMITS = new Supplier<Integer>( ) {
                                                                                     @Override
                                                                                     public Integer get( ) {
                                                                                       return Clusters.getConfiguration( )
                                                                                                      .getRequestWorkers( );
                                                                                     }
                                                                                   };
  private static final CacheLoader<InetAddress, Semaphore>  loader                 = new CacheLoader<InetAddress, Semaphore>( ) {
                                                                                     @Override
                                                                                     public Semaphore load( InetAddress key ) throws Exception {
                                                                                       return new Semaphore( CLUSTER_CLIENT_PERMITS.get(), true );
                                                                                     }
                                                                                   };
  private static final LoadingCache<InetAddress, Semaphore> counters               = CacheBuilder.newBuilder( ).build( loader );

  @Override
  public ChannelPipeline getPipeline( ) throws Exception {
    final ChannelHandler limitSockets = new SimpleChannelHandler( ) {
      private final String uuid = UUID.randomUUID( ).toString( );

      @Override
      public void writeRequested( ChannelHandlerContext ctx, MessageEvent e ) throws Exception {
        try {
          final MappingHttpRequest message = ( ( MappingHttpRequest ) e.getMessage() );
          final String logMessage = message.getMessage( ) != null
            ? message.getMessage( ).getClass( ).toString( ).replaceAll( "^.*\\.", "" ) : message.toString( );
          LOG.debug( Joiner.on( " " ).join( uuid,
                                            "writeRequested", ctx.getChannel( ),
                                            "message", logMessage ) );
        } catch ( Exception e1 ) {
          LOG.debug( e1 );
        }
        super.writeRequested( ctx, e );
      }
      
      /**
       * @see org.jboss.netty.channel.ChannelEvent()
       */
      @Override
      public void connectRequested( final ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
        try {
          /**
           * Get the semaphore for this remote address
           */
          final InetSocketAddress remoteAddress = ( ( InetSocketAddress ) e.getValue( ) );
          final Semaphore sem = counters.getUnchecked( remoteAddress.getAddress() );
          final int semAvailable = sem.availablePermits();
          final int semQueued = sem.getQueueLength();
          /**
           * Aquire permits from the semaphore for this remote address
           */
          final long start = System.nanoTime( );
          sem.acquire( );
          final long waitTime = System.nanoTime();
          e.getChannel( ).getCloseFuture( ).addListener( new ChannelFutureListener() {
            @Override
            public void operationComplete( ChannelFuture future ) throws Exception {
              try {
                final long end = System.nanoTime();
                LOG.trace( Joiner.on( " " ).join( uuid, remoteAddress,
                                                  String.format( "%d/%d+%d-queue", semAvailable, CLUSTER_CLIENT_PERMITS, semQueued ),
                                                  String.format( "%d+%d=%d-msec",
                                                                 TimeUnit.NANOSECONDS.toMillis( waitTime - start ),
                                                                 TimeUnit.NANOSECONDS.toMillis( end - waitTime ),
                                                                 TimeUnit.NANOSECONDS.toMillis( end - start ) )
                ) );
              } catch ( Exception e1 ) {
                LOG.trace( e1 );
              } finally {
                /**
                 * Ensure we release the permits for the semaphore for this remote address
                 */
                sem.release();
              }
            }
          } );
        } catch ( Exception e1 ) {
          LOG.trace( e1 );
        }
        super.connectRequested( ctx, e );
      }
      
    };
    final ChannelPipeline pipeline = Channels.pipeline( );
    for ( final Map.Entry<String, ChannelHandler> e : Handlers.channelMonitors( TimeUnit.SECONDS, StackConfiguration.CLIENT_INTERNAL_TIMEOUT_SECS ).entrySet( ) ) {
      pipeline.addLast( e.getKey( ), e.getValue( ) );
    }
    pipeline.addLast( "decoder", Handlers.newHttpResponseDecoder( ) );
    pipeline.addLast( "aggregator", Handlers.newHttpChunkAggregator( ) );
    pipeline.addLast( "encoder", Handlers.httpRequestEncoder( ) );
    pipeline.addLast( "serializer", Handlers.soapMarshalling( ) );
    pipeline.addLast( "wssec", wsSecHandler.get( ) );
    pipeline.addLast( "addressing", Handlers.newAddressingHandler( "EucalyptusCC#" ) );
    pipeline.addLast( "soap", Handlers.soapHandler( ) );
    pipeline.addLast( "binding", Handlers.bindingHandler( "eucalyptus_ucsb_edu" ) );
    pipeline.addLast( "gating", limitSockets );
    return pipeline;
  }
}
