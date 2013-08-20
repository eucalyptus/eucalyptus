/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.context;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.Channels;
import org.mule.RequestContext;
import org.mule.api.MuleMessage;
import com.eucalyptus.BaseException;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.ws.util.ReplyQueue;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessageSupplier;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import edu.ucsb.eucalyptus.msgs.HasRequest;
import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;

public class Contexts {
  private static Logger                          LOG             = Logger.getLogger( Contexts.class );
  private static int                             MAX             = 8192;
  private static int                             CONCUR          = MAX / ( Runtime.getRuntime( ).availableProcessors( ) * 2 + 1 );
  private static float                           THRESHOLD       = 1.0f;
  private static ConcurrentMap<String, Context>  uuidContexts    = new ConcurrentHashMap<String, Context>( MAX, THRESHOLD, CONCUR );
  private static ConcurrentMap<Channel, Context> channelContexts = new ConcurrentHashMap<Channel, Context>( MAX, THRESHOLD, CONCUR );
  
  static boolean hasOutstandingRequests( ) {
    return uuidContexts.keySet( ).size( ) > 0;
  }
  
  public static Context create( MappingHttpRequest request, Channel channel ) {
    Context ctx = new Context( request, channel );
    request.setCorrelationId( ctx.getCorrelationId( ) );
    uuidContexts.put( ctx.getCorrelationId( ), ctx );
    final Context previousContext = channelContexts.put( channel, ctx );
    if ( previousContext != null && previousContext.getCorrelationId() != null ) {
      uuidContexts.remove( previousContext.getCorrelationId() );
    }
    return ctx;
  }
  
  public static boolean exists( ) {
    try {
      lookup( );
      return true;
    } catch ( IllegalContextAccessException ex ) {
      return false;
    }
  }
  public static boolean exists( Channel channel ) {
    return channelContexts.containsKey( channel );
  }
  
  public static Context lookup( Channel channel ) throws NoSuchContextException {
    if ( !channelContexts.containsKey( channel ) ) {
      throw new NoSuchContextException( "Found channel context " + channel + " but no corresponding context." );
    } else {
      Context ctx = channelContexts.get( channel );
      ctx.setMuleEvent( RequestContext.getEvent( ) );
      return Context.maybeImpersonating( ctx );
    }
  }
  
  public static boolean exists( String correlationId ) {
    return correlationId != null && uuidContexts.containsKey( correlationId );
  }
  
  private static ThreadLocal<Context> tlContext = new ThreadLocal<Context>( );
  
  public static void threadLocal( Context ctx ) {//GRZE: really unhappy these are public.
    tlContext.set( ctx );
  }
  
  public static void removeThreadLocal( ) {//GRZE: really unhappy these are public.
    tlContext.remove( );
  }
  
  public static Context lookup( String correlationId ) throws NoSuchContextException {
    checkParam( "BUG: correlationId is null.", correlationId, notNullValue() );
    if ( !uuidContexts.containsKey( correlationId ) ) {
      throw new NoSuchContextException( "Found correlation id " + correlationId + " but no corresponding context." );
    } else {
      Context ctx = uuidContexts.get( correlationId );
      ctx.setMuleEvent( RequestContext.getEvent( ) );
      return Context.maybeImpersonating( ctx );
    }
  }
  
  public static final Context lookup( ) throws IllegalContextAccessException {
    Context ctx;
    if ( ( ctx = tlContext.get( ) ) != null ) {
      return Context.maybeImpersonating( ctx );
    }
    BaseMessage parent = null;
    MuleMessage muleMsg = null;
    if ( RequestContext.getEvent( ) != null && RequestContext.getEvent( ).getMessage( ) != null ) {
      muleMsg = RequestContext.getEvent( ).getMessage( );
    } else if ( RequestContext.getEventContext( ) != null && RequestContext.getEventContext( ).getMessage( ) != null ) {
      muleMsg = RequestContext.getEventContext( ).getMessage( );
    } else {
      throw new IllegalContextAccessException( "Cannot access context implicitly using lookup(V) outside of a service." );
    }
    Object o = muleMsg.getPayload( );
    if ( o != null && o instanceof BaseMessage ) {
      try {
        return Contexts.lookup( ( ( BaseMessage ) o ).getCorrelationId( ) );
      } catch ( NoSuchContextException e ) {
        Logs.exhaust( ).error( e, e );
        throw new IllegalContextAccessException( "Cannot access context implicitly using lookup(V) when not handling a request.", e );
      }
    } else if ( o != null && o instanceof HasRequest ) {
      try {
        return Contexts.lookup( ( ( HasRequest ) o ).getRequest( ).getCorrelationId( ) );
      } catch ( NoSuchContextException e ) {
        Logs.exhaust( ).error( e, e );
        throw new IllegalContextAccessException( "Cannot access context implicitly using lookup(V) when not handling a request.", e );
      }
    } else {
      throw new IllegalContextAccessException( "Cannot access context implicitly using lookup(V) when not handling a request." );
    }
  }
  
  public static void clear( String corrId ) {
    checkParam( "BUG: correlationId is null.", corrId, notNullValue() );
    Context ctx = uuidContexts.remove( corrId );
    Channel channel = null;
    if ( ctx != null && ( channel = ctx.getChannel( ) ) != null ) {
      channelContexts.remove( channel );
    } else {
      LOG.trace( "Context.clear() failed for correlationId=" + corrId );
      Logs.extreme( ).trace( "Context.clear() failed for correlationId=" + corrId, new RuntimeException( "Missing reference to channel for the request." ) );
    }
    if ( ctx != null ) {
      ctx.clear( );
    }
  }
  
  public static void clear( Context context ) {
    if ( context != null ) {
      clear( context.getCorrelationId( ) );
    }
  }
  
  public static Context createWrapped( String dest, final BaseMessage msg ) {
    if ( uuidContexts.containsKey( msg.getCorrelationId( ) ) ) {
      return null;
    } else {
      Context ctx = new Context( dest, msg );
      uuidContexts.put( ctx.getCorrelationId( ), ctx );
      return Context.maybeImpersonating( ctx );
    }
  }

  public static void response( BaseMessage responseMessage ) {
    response( responseMessage, responseMessage );
  }

  /**
   * Respond with the given supplier.
   * 
   * <p>This allows a response with associated details such as an HTTP status
   * code.</p>
   * 
   * @param responseMessageSupplier The supplier to use
   */
  public static void response( final BaseMessageSupplier responseMessageSupplier ) {
    response( responseMessageSupplier, responseMessageSupplier.getBaseMessage() );  
  }
  
  @SuppressWarnings( "unchecked" )
  private static void response( final Object message,
                                final BaseMessage responseMessage ) {
    if ( responseMessage instanceof ExceptionResponseType ) {
      Logs.exhaust( ).trace( responseMessage );
    }
    String corrId = responseMessage.getCorrelationId( );
    try {
      Context ctx = lookup( corrId );
      EventRecord.here( ServiceContext.class, EventType.MSG_REPLY, responseMessage.getCorrelationId( ), responseMessage.getClass( ).getSimpleName( ),
                        String.format( "%.3f ms", ( System.nanoTime( ) - ctx.getCreationTime( ) ) / 1000000.0 ) ).trace( );
      Channel channel = ctx.getChannel( );
      Channels.write( channel, message );
      clear( ctx );
    } catch ( NoSuchContextException e ) {
      LOG.warn( "Received a reply for absent client:  No channel to write response message: " + e.getMessage( ) );
      Logs.extreme( ).debug( responseMessage, e );
    } catch ( Exception e ) {
      LOG.warn( "Error occurred while handling reply: " + responseMessage );
      Logs.extreme( ).debug( responseMessage, e );
    }
  }

  public static void responseError( Throwable cause ) {
    try {
      Contexts.responseError( lookup( ).getCorrelationId( ), cause );
    } catch ( Exception e ) {
      LOG.error( e );
      Logs.extreme( ).error( cause, cause );
    }
  }

  public static void responseError( String corrId, Throwable cause ) {
    try {
      Context ctx = lookup( corrId );
      EventRecord.here( ReplyQueue.class, EventType.MSG_REPLY, cause.getClass( ).getCanonicalName( ), cause.getMessage( ),
                        String.format( "%.3f ms", ( System.nanoTime( ) - ctx.getCreationTime( ) ) / 1000000.0 ) ).trace( );
      Channels.fireExceptionCaught( ctx.getChannel( ), cause );
      if ( !( cause instanceof BaseException ) ) {
        clear( ctx );
      }
    } catch ( Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( cause, cause );
    }
  }
  
}
