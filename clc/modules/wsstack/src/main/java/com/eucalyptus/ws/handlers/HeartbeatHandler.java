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
package com.eucalyptus.ws.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.NetworkUtil;
import com.eucalyptus.ws.MappingHttpResponse;
import com.eucalyptus.ws.stages.UnrollableStage;

@ChannelPipelineCoverage("one")
public class HeartbeatHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler, UnrollableStage {
  private static Logger LOG = Logger.getLogger( HeartbeatHandler.class );
  private Channel channel;
  private static boolean initialized = false;
  
  
  public HeartbeatHandler( ) {
    super( );
    initialized = true;
  }

  public HeartbeatHandler( Channel channel ) {
    super( );
    this.channel = channel;
  }

  @Override
  public void handleDownstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    ctx.sendDownstream( e );
  }

  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent ) {
      Object message = ( ( MessageEvent ) e ).getMessage( );
      if ( message instanceof HttpRequest ) {
        HttpRequest request = ( ( HttpRequest ) message );
        if( !initialized ) {
          initialize( ctx, request );
        } 
      }
    }
  }

  private void initialize( ChannelHandlerContext ctx, HttpRequest request ) throws IOException, SocketException {
    ByteArrayInputStream bis = new ByteArrayInputStream( request.getContent( ).toByteBuffer( ).array( ) );
    Properties props = new Properties( );
    props.load( bis );
    InetSocketAddress addr = (InetSocketAddress) ctx.getChannel( ).getRemoteAddress( );//this is the db address
    LOG.info( LogUtil.subheader( "Using "+addr.getHostName( )+" as the database address." ));
    Component.db.setHostAddress( addr.getHostName( ) );
    System.setProperty( "euca.db.password", "" );
    System.setProperty( "euca.db.url", Component.db.getUri( ).toASCIIString( ) );
    this.channel.close( );

    boolean foundDb = false;
    List<String> localAddrs = NetworkUtil.getAllAddresses( );
    for ( Entry<Object, Object> entry : props.entrySet( ) ) {
      String key = (String)entry.getKey();
      String value = (String)entry.getValue();
      if( key.startsWith("euca.db.host") ) {
        try {
          if( NetworkUtil.testReachability( value ) && !localAddrs.contains( value )) {
            LOG.info( "Found candidate db host address: " + value );
            String oldValue = System.setProperty( "euca.db.host", value );
            LOG.info( "Setting property: euca.db.host=" + value + " [oldvalue="+oldValue+"]" );              
            Component.db.setHostAddress( value );
            //TODO: test we can connect here.
            foundDb = true;
          }
        } catch ( Exception e1 ) {
          LOG.warn( "Ignoring proposed database address: " + value );
        }
      } else {
        String oldValue = System.setProperty( ( String ) entry.getKey( ), ( String ) entry.getValue( ) );
        LOG.info( "Setting property: " + entry.getKey( ) + "=" + entry.getValue( ) + " [oldvalue="+oldValue+"]" );
      }
    }
    if( foundDb ) {
      ChannelFuture writeFuture = ctx.getChannel( ).write( new DefaultHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.OK ) );
      writeFuture.addListener( ChannelFutureListener.CLOSE );
      if( this.channel != null ) {
        this.channel.close();
      }
    } else {
      ChannelFuture writeFuture = ctx.getChannel( ).write( new DefaultHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.NOT_ACCEPTABLE ) );
      writeFuture.addListener( ChannelFutureListener.CLOSE );          
    }
  }

  @Override
  public String getStageName( ) {
    return "heartbeat";
  }

  @Override
  public void unrollStage( ChannelPipeline pipeline ) {
    pipeline.addLast( "heartbeat", new HeartbeatHandler( ) );
  }

}
