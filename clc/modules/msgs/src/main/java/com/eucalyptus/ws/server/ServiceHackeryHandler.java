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

package com.eucalyptus.ws.server;

import java.util.UUID;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpChunk;
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
import edu.ucsb.eucalyptus.msgs.BaseMessageSupplier;
import edu.ucsb.eucalyptus.msgs.StreamedBaseMessage;

@ChannelHandler.Sharable
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
      } else if ( msge.getMessage( ) instanceof IsData || msge.getMessage() instanceof HttpChunk ) {// Pass through for chunked messaging
        ctx.sendDownstream( e );
      } else if ( msge.getMessage( ) instanceof BaseMessage ) {// Handle single request-response MEP
        BaseMessage reply = ( BaseMessage ) ( ( MessageEvent ) e ).getMessage( );
        
        //Added by zhill to generalize the walrus streaming responses to break build dependencies.
        if ( reply instanceof StreamedBaseMessage && !( ((StreamedBaseMessage)reply).getHasStreamingData())) {
        	e.getFuture().cancel();
        	return;
        } else {
        	ctx.sendDownstream(msge);        	
        }        
        /*
         * if ( reply instanceof WalrusDataGetResponseType //TODO:GRZE:FIXME:FIXME:FIXME:WTF
             && !( reply instanceof GetObjectResponseType && ( ( GetObjectResponseType ) reply ).getBase64Data( ) != null ) ) {
        	//for walrus data responses, but not those that have data in them, cancel the future that would close the channel.
          e.getFuture( ).cancel( );
          return;
        } else {
          ctx.sendDownstream( msge );
        }*/
      } else if ( msge.getMessage( ) instanceof BaseMessageSupplier ) {// Handle single request-response MEP
        ctx.sendDownstream( msge );
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
