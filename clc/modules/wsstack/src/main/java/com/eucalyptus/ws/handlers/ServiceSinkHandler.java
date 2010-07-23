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
package com.eucalyptus.ws.handlers;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.DefaultMuleSession;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.transport.AbstractConnector;
import org.mule.transport.NullPayload;
import org.mule.transport.vm.VMMessageDispatcherFactory;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.client.NioMessageReceiver;
import com.eucalyptus.ws.util.ReplyQueue;
import edu.ucsb.eucalyptus.constants.IsData;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.GetObjectResponseType;
import edu.ucsb.eucalyptus.msgs.WalrusDataGetResponseType;

@ChannelPipelineCoverage( "one" )
public class ServiceSinkHandler extends SimpleChannelHandler {
  private static VMMessageDispatcherFactory dispatcherFactory = new VMMessageDispatcherFactory( );
  private static Logger                     LOG               = Logger.getLogger( ServiceSinkHandler.class );
  private AtomicLong                        startTime         = new AtomicLong( 0l );
  
  private NioMessageReceiver                msgReceiver;
  
  public ServiceSinkHandler( ) {}
  
  public ServiceSinkHandler( final NioMessageReceiver msgReceiver ) {
    this.msgReceiver = msgReceiver;
  }
  
  @Override
  public void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent e ) {//FIXME: handle exceptions cleanly.
    LOG.trace( ctx.getChannel( ), e.getCause( ) );
    Channels.fireExceptionCaught( ctx.getChannel( ), e.getCause( ) );
  }
  
  @SuppressWarnings( "unchecked" )
  @Override
  public void handleDownstream( final ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    LOG.trace( this.getClass( ).getSimpleName( ) + "[outgoing]: " + e.getClass( ) );
    if ( e instanceof MessageEvent ) {
      final MessageEvent msge = ( MessageEvent ) e;
      if ( msge.getMessage( ) instanceof NullPayload ) {
        msge.getFuture( ).cancel( );
      } else if ( msge.getMessage( ) instanceof HttpResponse ) {
        ctx.sendDownstream( e );
      } else if ( msge.getMessage( ) instanceof IsData ) {// Pass through for chunked messaging
        ctx.sendDownstream( e );
      } else if ( msge.getMessage( ) instanceof BaseMessage ) {// Handle single request-response MEP
        BaseMessage reply = ( BaseMessage ) ( ( MessageEvent ) e ).getMessage( );
        if ( reply instanceof WalrusDataGetResponseType
             && !( reply instanceof GetObjectResponseType && ( ( GetObjectResponseType ) reply ).getBase64Data( ) != null ) ) {
          e.getFuture( ).cancel( );
          return;
        } else {
          this.sendDownstreamNewEvent( ctx, e, reply );
        }
      } else {
        e.getFuture( ).cancel( );
        LOG.warn( "Non-specific type being written to the channel. Not dropping this message causes breakage:" + msge.getMessage( ).getClass( ) );
      }
      if ( e.getFuture( ).isCancelled( ) ) {
        LOG.trace( "Cancelling send on : " + LogUtil.dumpObject( e ) );
      }
    } else {
      ctx.sendDownstream( e );
    }
  }
  
  private void sendDownstreamNewEvent( ChannelHandlerContext ctx, ChannelEvent e, BaseMessage reply ) {
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
      EventRecord.here( reply.getClass( ), EventClass.MESSAGE, EventType.MSG_SERVICED, Long.toString( System.currentTimeMillis( ) - this.startTime.get( ) ) ).trace();
      final MappingHttpResponse response = new MappingHttpResponse( request.getProtocolVersion( ) );
      final DownstreamMessageEvent newEvent = new DownstreamMessageEvent( ctx.getChannel( ), e.getFuture( ), response, null );
      response.setMessage( reply );
      ctx.sendDownstream( newEvent );
//      Contexts.clear( reqCtx );
    }
  }
  
  @Override
  public void handleUpstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
    LOG.trace( this.getClass( ).getSimpleName( ) + "[incoming]: " + e );
    if ( e instanceof ExceptionEvent ) {
      this.exceptionCaught( ctx, ( ExceptionEvent ) e );
    } else if ( e instanceof MessageEvent ) {
      this.startTime.compareAndSet( 0l, System.currentTimeMillis( ) );
      final MessageEvent event = ( MessageEvent ) e;
      if ( event.getMessage( ) instanceof MappingHttpMessage ) {
        final MappingHttpMessage request = ( MappingHttpMessage ) event.getMessage( );
        final User user = Contexts.lookup( request.getCorrelationId( ) ).getUser( );
        final BaseMessage msg = ( BaseMessage ) request.getMessage( );
        final String userAgent = request.getHeader( HttpHeaders.Names.USER_AGENT );
        if ( msg.getCorrelationId( ) == null ) {
          String corrId = null;
          try {
            corrId = Contexts.lookup( ctx.getChannel( ) ).getCorrelationId( );
          } catch ( Exception e1 ) {
            corrId = UUID.randomUUID( ).toString( );
          }
          msg.setCorrelationId( corrId );
        }
        if ( ( userAgent != null ) && userAgent.matches( ".*EucalyptusAdminAccess" ) && msg.getClass( ).getSimpleName( ).startsWith( "Describe" ) ) {
          msg.setEffectiveUserId( msg.getUserId( ) );
        } else if ( ( user != null ) && ( this.msgReceiver == null ) ) {
          msg.setUserId( user.getName( ) );
          msg.setEffectiveUserId( user.isAdministrator( ) ? Component.eucalyptus.name( ) : user.getName( ) );
        }
        EventRecord.here( ServiceSinkHandler.class, EventType.MSG_RECEIVED, msg.getClass( ).getSimpleName( ) ).trace( );
        if ( this.msgReceiver == null ) {
          ServiceSinkHandler.dispatchRequest( msg );
        } else if ( ( user == null ) || ( ( user != null ) && user.isAdministrator( ) ) ) {
          this.dispatchRequest( ctx, request, msg );
        } else {
          Contexts.clear( Contexts.lookup( msg.getCorrelationId( ) ) );
          ctx.getChannel( ).write( new MappingHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.FORBIDDEN ) );
        }
      } else if ( e instanceof IdleStateEvent ) {
        LOG.warn( "Closing idle connection: " + e );
        e.getFuture( ).addListener( ChannelFutureListener.CLOSE );
        ctx.sendUpstream( e );
      }
      
    }
  }
  
  private void dispatchRequest( final ChannelHandlerContext ctx, final MappingHttpMessage request, final BaseMessage msg ) throws NoSuchContextException {
    try {
      final MuleMessage reply = this.msgReceiver.routeMessage( new DefaultMuleMessage( msg ), true );
      if ( reply != null ) {
        ReplyQueue.handle( this.msgReceiver.getService( ).getName( ), reply, msg );
      } else {
        EventRecord.here( ServiceSinkHandler.class, EventType.MSG_SENT_ASYNC, msg.getClass( ).getSimpleName( ), this.msgReceiver.getEndpointURI( ).toString( ) );
      }
    } catch ( Exception e1 ) {
      LOG.error( e1, e1 );
      EucalyptusErrorMessageType errMsg = new EucalyptusErrorMessageType( this.msgReceiver.getService( ).getName( ), msg,
                                                                          ( e1.getCause( ) != null ? e1.getCause( ) : e1 ).getMessage( ) );
      errMsg.setCorrelationId( msg.getCorrelationId( ) );
      errMsg.setException( e1.getCause( ) != null ? e1.getCause( ) : e1 );
      Contexts.clear( Contexts.lookup( errMsg.getCorrelationId( ) ) );
      Channels.write( ctx.getChannel( ), errMsg );
    }
  }
  
  private static void dispatchRequest( final BaseMessage msg ) throws MuleException, DispatchException {
    OutboundEndpoint endpoint = ServiceContext.getContext( ).getRegistry( ).lookupEndpointFactory( ).getOutboundEndpoint( "vm://RequestQueue" );
    if ( !endpoint.getConnector( ).isStarted( ) ) {
      endpoint.getConnector( ).start( );
    }
    MuleMessage muleMsg = new DefaultMuleMessage( msg );
    MuleSession muleSession = new DefaultMuleSession( muleMsg, ( ( AbstractConnector ) endpoint.getConnector( ) ).getSessionHandler( ),
                                                      ServiceContext.getContext( ) );
    MuleEvent muleEvent = new DefaultMuleEvent( muleMsg, endpoint, muleSession, false );
    dispatcherFactory.create( endpoint ).dispatch( muleEvent );
  }
  
  @Override
  public void channelClosed( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
    try {
      Contexts.clear( Contexts.lookup( ctx.getChannel( ) ) );
    } catch ( Throwable e1 ) {
      LOG.warn( "Failed to remove the channel context on connection close.", e1 );
    }
    super.channelClosed( ctx, e );
  }
  
  @Override
  public void messageReceived( ChannelHandlerContext ctx, MessageEvent e ) throws Exception {
    super.messageReceived( ctx, e );
  }
  
}
