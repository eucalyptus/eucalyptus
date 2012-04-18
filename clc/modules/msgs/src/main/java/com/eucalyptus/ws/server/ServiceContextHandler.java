/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.ws.server;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import com.eucalyptus.component.ServiceOperations;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.context.ServiceDispatchException;
import com.eucalyptus.context.ServiceInitializationException;
import com.eucalyptus.context.ServiceStateException;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.ws.util.RequestQueue;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;

@ChannelPipelineCoverage( "one" )
public class ServiceContextHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler {
  private static Logger             LOG         = Logger.getLogger( ServiceContextHandler.class );
  private ChannelLocal<Long>        startTime   = new ChannelLocal<Long>( );
  private ChannelLocal<Long>        openTime    = new ChannelLocal<Long>( );
  private ChannelLocal<BaseMessage> messageType = new ChannelLocal<BaseMessage>( ) {
                                                  
                                                  @Override
                                                  protected BaseMessage initialValue( Channel channel ) {
                                                    return new BaseMessage( );
                                                  }
                                                };
  
  public void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent e ) {//FIXME:GRZE: handle exceptions cleanly. convert to msg type and write.
    LOG.debug( ctx.getChannel( ), e.getCause( ) );
  }
  
  @SuppressWarnings( "unchecked" )
  @Override
  public void handleDownstream( final ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    if ( Logs.isExtrrreeeme( ) ) LOG.trace( this.getClass( ).getSimpleName( ) + "[outgoing]: " + e.getClass( ) );
    BaseMessage reply = BaseMessage.extractMessage( e );
    if ( reply instanceof BaseMessage ) {
      MessageEvent newEvent = makeDownstreamNewEvent( ctx, e, reply );
      ctx.sendDownstream( newEvent );
    } else if ( e instanceof ExceptionEvent ) {
      exceptionCaught( ctx, ( ExceptionEvent ) e );
      ctx.sendDownstream( e );
    } else {
      ctx.sendDownstream( e );
    }
  }
  
  private MessageEvent makeDownstreamNewEvent( ChannelHandlerContext ctx, ChannelEvent e, BaseMessage reply ) {
    MappingHttpRequest request = null;
    Context reqCtx = null;
    try {
      if ( reply != null ) {
        reqCtx = Contexts.lookup( reply.getCorrelationId( ) );
        request = reqCtx.getHttpRequest( );
      }
    } catch ( NoSuchContextException e1 ) {
      LOG.debug( e1, e1 );
    }
    if ( request != null ) {
      if ( reply == null ) {
        LOG.warn( "Received a null response for request: " + request.getMessageString( ) );
        reply = new EucalyptusErrorMessageType( this.getClass( ).getSimpleName( ), ( BaseMessage ) request.getMessage( ), "Received a NULL reply" );
      }
      Long currTime = System.currentTimeMillis( );
      try {
    	  Logs.extreme( ).debug( EventRecord.here( reply.getClass( ), EventClass.MESSAGE, EventType.MSG_SERVICED, "request-ms",
    			  Long.toString( currTime - this.startTime.get( ctx.getChannel( ) ) ) ) );
      } catch ( Exception ex ) {
    	  Logs.extreme( ).trace( ex, ex );
      }
      final MappingHttpResponse response = new MappingHttpResponse( request.getProtocolVersion( ) );
      final DownstreamMessageEvent newEvent = new DownstreamMessageEvent( ctx.getChannel( ), e.getFuture( ), response, null );
      response.setMessage( reply );
      return newEvent;
//      Contexts.clear( reqCtx );
    } else {
      final MappingHttpResponse response = new MappingHttpResponse( HttpVersion.HTTP_1_1 ) {
        {
          setMessage( new EucalyptusErrorMessageType( this.getClass( ).getSimpleName( ), "Received a NULL reply" ) );
        }
      };
      final DownstreamMessageEvent newEvent = new DownstreamMessageEvent( ctx.getChannel( ), e.getFuture( ), response, null );
      return newEvent;
    }
  }
  
  @Override
  public void handleUpstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
    final MappingHttpMessage request = MappingHttpMessage.extractMessage( e );
    final BaseMessage msg = BaseMessage.extractMessage( e );
    if ( Logs.isExtrrreeeme( ) ) LOG.trace( this.getClass( ).getSimpleName( ) + "[incoming]:" + ( msg != null
      ? msg.getClass( ).getSimpleName( )
      : "" ) + " " + e );
    
    if ( e instanceof ChannelStateEvent ) {
      ChannelStateEvent evt = ( ChannelStateEvent ) e;
      switch ( evt.getState( ) ) {
        case OPEN:
          if ( Boolean.TRUE.equals( evt.getValue( ) ) ) {
            this.channelOpened( ctx, evt );
          } else {
            this.channelClosed( ctx, evt );
          }
        case BOUND:
        case CONNECTED:
        case INTEREST_OPS:
        default:
          Logs.extreme( ).trace( "Channel event: " + evt );
          ctx.sendUpstream( e );
      }
    } else if ( e instanceof IdleStateEvent ) {
      Logs.extreme( ).warn( "Closing idle connection: " + e );
      e.getFuture( ).addListener( ChannelFutureListener.CLOSE );
      ctx.sendUpstream( e );
    } else if ( request != null && msg != null ) {
      this.messageReceived( ctx, msg );
      ctx.sendUpstream( e );
    } else if ( e instanceof ExceptionEvent ) {
      this.exceptionCaught( ctx, ( ExceptionEvent ) e );
      ctx.sendUpstream( e );
    } else {
      ctx.sendUpstream( e );
    }
  }
  
  private void messageReceived( final ChannelHandlerContext ctx, final BaseMessage msg ) throws ServiceDispatchException {
    this.startTime.set( ctx.getChannel( ), System.currentTimeMillis( ) );
    this.messageType.set( ctx.getChannel( ), msg );
    EventRecord.here( ServiceContextHandler.class, EventType.MSG_RECEIVED, msg.getClass( ).getSimpleName( ) ).trace( );
    ServiceOperations.dispatch( msg );
  }
  
  private void channelClosed( ChannelHandlerContext ctx, ChannelStateEvent evt ) {
    if ( Contexts.exists( ctx.getChannel( ) ) ) {
      try {
        Contexts.clear( Contexts.lookup( ctx.getChannel( ) ) );
      } catch ( NoSuchContextException ex ) {
        Logs.extreme( ).debug( "Failed to remove the channel context on connection close.", ex );
      }
    }
    try {
      Logs.extreme( ).debug( EventRecord.here( this.messageType.getClass( ), EventClass.MESSAGE, EventType.MSG_SERVICED, "rtt-ms", Long.toString( System.currentTimeMillis( ) - this.openTime.get( ctx.getChannel( ) ) ) ) );
    } catch ( Exception ex ) {
      Logs.extreme( ).trace( ex, ex );
    }
  }
  
  private void channelOpened( final ChannelHandlerContext ctx, ChannelStateEvent evt ) {
    this.openTime.set( ctx.getChannel( ), System.currentTimeMillis( ) );
  }
  
}
