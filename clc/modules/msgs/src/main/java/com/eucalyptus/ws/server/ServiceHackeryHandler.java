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
 ************************************************************************/

package com.eucalyptus.ws.server;

import java.util.UUID;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.mule.transport.NullPayload;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.LogUtil;
import edu.ucsb.eucalyptus.constants.IsData;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.GetObjectResponseType;
import edu.ucsb.eucalyptus.msgs.WalrusDataGetResponseType;

@ChannelPipelineCoverage( "all" )
public enum ServiceHackeryHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler {
  INSTANCE;
  private static Logger LOG = Logger.getLogger( ServiceHackeryHandler.class );
  
//  @Override
//  public void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent e ) {//FIXME: handle exceptions cleanly.
//    LOG.trace( ctx.getChannel( ), e.getCause( ) );
//    Channels.fireExceptionCaught( ctx.getChannel( ), e.getCause( ) );
//  }
  
  @SuppressWarnings( "unchecked" )
  @Override
  public void handleDownstream( final ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent ) {
      final MessageEvent msge = ( MessageEvent ) e;
      if ( msge.getMessage( ) instanceof NullPayload ) {
        LOG.error( "Received NULL response: " + ( ( NullPayload ) msge.getMessage( ) ).toString( ) );
        msge.getFuture( ).cancel( );
      } else if ( msge.getMessage( ) instanceof HttpResponse ) {
        ctx.sendDownstream( e );
      } else if ( msge.getMessage( ) instanceof IsData ) {// Pass through for chunked messaging
        ctx.sendDownstream( e );
      } else if ( msge.getMessage( ) instanceof BaseMessage ) {// Handle single request-response MEP
        BaseMessage reply = ( BaseMessage ) ( ( MessageEvent ) e ).getMessage( );
        if ( reply instanceof WalrusDataGetResponseType //TODO:GRZE:FIXME:FIXME:FIXME:WTF
             && !( reply instanceof GetObjectResponseType && ( ( GetObjectResponseType ) reply ).getBase64Data( ) != null ) ) {
          e.getFuture( ).cancel( );
          return;
        } else {
          ctx.sendDownstream( msge );
        }
      } else if ( msge.getMessage( ) instanceof Throwable ) {
        ctx.sendDownstream( e );
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
  
  @Override
  public void handleUpstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
    final MappingHttpMessage request = MappingHttpMessage.extractMessage( e );
    final BaseMessage msg = BaseMessage.extractMessage( e );
    if ( request != null && msg != null ) {
      EventRecord.here( ServiceHackeryHandler.class, EventType.MSG_RECEIVED, msg.getClass( ).getSimpleName( ) ).trace( );
      final User user = Contexts.lookup( request.getCorrelationId( ) ).getUser( );
      
      this.mangleCorrelationId( ctx, msg );
      this.mangleAdminDescribe( request, msg, user );
      
      ctx.sendUpstream( e );
    } else {
      ctx.sendUpstream( e );
    }
  }
  
  private void mangleCorrelationId( final ChannelHandlerContext ctx, final BaseMessage msg ) {//TODO:ASAP:GRZE wth is this for???
    if ( msg.getCorrelationId( ) == null ) {
      String corrId = null;
      try {
        corrId = Contexts.lookup( ctx.getChannel( ) ).getCorrelationId( );
      } catch ( Exception e1 ) {
        corrId = UUID.randomUUID( ).toString( );
      }
      msg.setCorrelationId( corrId );
    }
  }
  
  private void mangleAdminDescribe( final MappingHttpMessage request, final BaseMessage msg, final User user ) {//TODO:ASAP:GRZE fix this mangling somewhere else.
    final String userAgent = request.getHeader( HttpHeaders.Names.USER_AGENT );
    if ( ( userAgent != null ) && userAgent.matches( ".*EucalyptusAdminAccess" ) && msg.getClass( ).getSimpleName( ).startsWith( "Describe" ) ) {
      msg.markUnprivileged( );//TODO:GRZE:FIXME this doesn't do what it used to
    }
  }
  
}
