/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************ 
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.ws.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpMethod;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ComponentMessages;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.ws.protocol.OperationParameter;
import com.eucalyptus.ws.protocol.RequiredQueryParams;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ChannelHandler.Sharable
public enum ServiceAccessLoggingHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler {
  INSTANCE;
  
  public static Set<String> ignoredParameters          = new TreeSet<String>( ) {
                                                         {
                                                           this.addAll( Collections2.transform( Arrays.asList( OperationParameter.values( ) ),
                                                                                                Functions.toStringFunction( ) ) );
                                                           this.addAll( Collections2.transform( Arrays.asList( SecurityParameter.values( ) ),
                                                                                                Functions.toStringFunction( ) ) );
                                                           this.addAll( Collections2.transform( Arrays.asList( RequiredQueryParams.values( ) ),
                                                                                                Functions.toStringFunction( ) ) );
                                                         }
                                                       };
  
  private static final//
  Predicate<String>         ignoredParametersPredicate = new Predicate<String>( )
                                                       
                                                       {
                                                         @Override
                                                         public boolean apply( String input ) {
                                                           try {
                                                             return !ignoredParameters.contains( input.substring( 0, input.indexOf( "=" ) ) );
                                                           } catch ( Exception e ) {
                                                             return false;
                                                           }
                                                         }
                                                       };
  private static final// 
  Function<MappingHttpRequest, //
  Collection<String>>      //
                            createParameters           = new Function<MappingHttpRequest, Collection<String>>( ) {
                                                         @Override
                                                         public Collection<String> apply( MappingHttpRequest input ) {
                                                           try {
                                                             String query = Objects.toString( input.getQuery( ),
                                                                                              "" );
                                                             if ( HttpMethod.POST.equals( input.getMethod( ) ) ) {
                                                               final ChannelBuffer buffer = input.getContent( );
                                                               buffer.markReaderIndex( );
                                                               query = new String( buffer.array( ) );
                                                               buffer.resetReaderIndex( );
                                                             }
                                                             return Collections2.filter( Arrays.asList( query.split( "&" ) ),
                                                                                         ignoredParametersPredicate );
                                                           } catch ( Exception e ) {
                                                             return Collections.EMPTY_LIST;
                                                           }
                                                         }
                                                       };
  
  private static Logger     LOG                        = Logger.getLogger( ServiceAccessLoggingHandler.class );
  
  private static void createLogEntry( ChannelHandlerContext ctx, Object replyObject, String... extra ) {
    try {
      Context context = Contexts.lookup( ctx.getChannel( ) );
      List<String> logEntries = Lists.newArrayList( context.getCorrelationId( ),
                                                    context.getUserFullName( ).toString( ),
                                                    ctx.getChannel( ).getRemoteAddress( ).toString( ),
                                                    ctx.getChannel( ).getLocalAddress( ).toString( )
                                     );
      String componentName = "wsstack";//GRZE: this basically means unattributable to a component... sigh.
      if ( replyObject instanceof BaseMessage ) {
        try {
          final Class<? extends ComponentId> compMsg = ComponentMessages.lookup( ( BaseMessage ) replyObject );
          final ComponentId compId = ComponentIds.lookup( compMsg );
          componentName = compId.name( );
        } catch ( Exception e ) {
          //GRZE: this needs to be a separate logger to avoid hitting the configured category for this class.
          Logger.getLogger( "LoggingError." + ServiceAccessLoggingHandler.class.getCanonicalName( ) ).debug( e );
        }
      }
      logEntries.add( componentName );
      logEntries.add( replyObject.getClass( ).getSimpleName( ) );
      logEntries.add( "[" + Joiner.on( "," ).join( Arrays.asList( extra ) ) + "]" );
      LOG.info( Joiner.on( " " ).join( logEntries ) );
    } catch ( Exception e ) {
      //GRZE: this needs to be a separate logger to avoid hitting the configured category for this class.
      Logger.getLogger( "LoggingError." + ServiceAccessLoggingHandler.class.getCanonicalName( ) ).debug( e );
    }
  }
  
  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    try {
      if ( e instanceof MessageEvent ) {
        final MessageEvent msge = ( MessageEvent ) e;
        if ( msge.getMessage( ) instanceof MappingHttpRequest ) {// Handle single request-response MEP
          MappingHttpRequest httpMessage = ( MappingHttpRequest ) ( ( MessageEvent ) e ).getMessage( );
          final String method = Objects.toString( httpMessage.getMethod( ), "HTTP-UNKNOWN" );
          final String parameters = Joiner.on( "," ).join( createParameters.apply( httpMessage ) );
          createLogEntry( ctx, httpMessage.getMessage( ), method, parameters );
        }
      }
    } catch ( Exception e1 ) {
      Logger.getLogger( "LoggingError." + ServiceAccessLoggingHandler.class.getCanonicalName( ) ).debug( e1 );
    }
    ctx.sendUpstream( e );
  }
  
  @Override
  public void handleDownstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    try {
      if ( e instanceof MessageEvent ) {
        final MessageEvent msge = ( MessageEvent ) e;
        if ( msge.getMessage( ) instanceof MappingHttpResponse ) {// Handle single request-response MEP
          MappingHttpResponse httpMessage = ( MappingHttpResponse ) ( ( MessageEvent ) e ).getMessage( );
          createLogEntry( ctx, httpMessage.getMessage( ), Objects.toString( httpMessage.getStatus(), "UNKNOWN" ) );
        }
      } else if ( e instanceof ExceptionEvent ) {
        Throwable reply = ( ( ExceptionEvent ) e ).getCause( );
        String[] extra = new String[]{};
        try {
          final List<Throwable> causalChain = Throwables.getCausalChain( reply );
          extra = ( String[] ) Collections2.transform( causalChain, Functions.toStringFunction() ).toArray();
        } catch ( Exception e1 ) {
          Logger.getLogger( "LoggingError." + ServiceAccessLoggingHandler.class.getCanonicalName( ) ).debug( e1 );
        }
        createLogEntry( ctx, reply, extra );
      }
    } catch ( Exception e1 ) {
      Logger.getLogger( "LoggingError." + ServiceAccessLoggingHandler.class.getCanonicalName( ) ).debug( e1 );
    }
    ctx.sendDownstream( e );
  }
  
}
