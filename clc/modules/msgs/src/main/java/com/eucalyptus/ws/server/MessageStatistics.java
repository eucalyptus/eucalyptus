/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
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
 ************************************************************************/

package com.eucalyptus.ws.server;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.records.Logs;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Callables;

public class MessageStatistics {
  private static Logger                            LOG               = Logger.getLogger( MessageStatistics.class );
  private static final Map<Integer, RequestRecord> requestStatistics = Maps.newConcurrentMap( );
  
  private static class HandlerRecord implements Callable<Long> {
    private final String handlerClassName;
    private final Long   startTime = System.currentTimeMillis( );
    private Long         endTime;
    
    private HandlerRecord( Class<? extends ChannelHandler> class1 ) {
      this.handlerClassName = class1.toString( );
    }
    
    @Override
    public String toString( ) {
      StringBuilder builder = new StringBuilder( );
      Long time = ( ( this.endTime == null ? System.currentTimeMillis( ) : this.endTime ) - this.startTime );
      builder.append( " " );
      builder.append( this.handlerClassName ).append( ": " ).append( time ).append( " msec" );
      return builder.toString( );
    }
    
    @Override
    public Long call( ) throws Exception {
      Logs.extreme( ).debug( "HandlerRecord:" + this.handlerClassName
        + " "
        + ( ( this.endTime == null ? this.endTime = System.currentTimeMillis( ) : this.endTime ) - this.startTime )
        + "msec" );
      return this.endTime;
    }
    
  }
  
  private static class RequestRecord {
    private final Map<Class, HandlerRecord> handlerUpstreamStats   = Maps.newHashMap( );
    private final Map<Class, HandlerRecord> handlerDownstreamStats = Maps.newHashMap( );
    String                                  type;
    private final long                      creationTime           = System.currentTimeMillis( );
    
    public Collection<HandlerRecord> values( ) {
      return this.handlerUpstreamStats.values( );
    }
    
    @Override
    public String toString( ) {
      StringBuilder builder = new StringBuilder( );
      String typeName = ( this.type == null ? "unknown" : this.type );
      builder.append( typeName ).append( " " ).append( Joiner.on( "\n" + typeName ).join( this.handlerUpstreamStats.values( ) ) );
      builder.append( typeName ).append( " " ).append( Joiner.on( "\n" + typeName ).join( this.handlerDownstreamStats.values( ) ) );
      builder.append( typeName ).append( " " ).append( System.currentTimeMillis( ) - this.creationTime ).append( " msec" );
      return builder.toString( );
    }
    
  }
  
  public static final void startRequest( final Channel channel ) {
    if ( StackConfiguration.STATISTICS ) {
      final RequestRecord record = new RequestRecord( );
      requestStatistics.put( channel.getId( ), record );
      channel.getCloseFuture( ).addListener( new ChannelFutureListener( ) {
        
        @Override
        public void operationComplete( ChannelFuture future ) throws Exception {
          LOG.info( requestStatistics.remove( channel.getId( ) ) );
        }
      } );
    }
  }
  
  public static final <T extends ChannelHandler> Callable<Long> startUpstream( Channel channel, T handler ) {
    Integer correlationId = channel.getId( );
    if ( StackConfiguration.STATISTICS && requestStatistics.containsKey( correlationId ) ) {
      RequestRecord record = requestStatistics.get( correlationId );
      if ( record.type == null ) {
        try {
          record.type = Contexts.lookup( channel ).getRequest( ).getClass( ).getSimpleName( );
        } catch ( Exception ex ) {}
      }
      HandlerRecord handlerRecord = new HandlerRecord( handler.getClass( ) );
      record.handlerUpstreamStats.put( handler.getClass( ), handlerRecord );
      return handlerRecord;
    } else {
      return Callables.returning( 0L );
    }
  }
  
  public static final <T extends ChannelHandler> Callable<Long> startDownstream( Channel channel, T handler ) {
    Integer correlationId = channel.getId( );
    if ( StackConfiguration.STATISTICS && requestStatistics.containsKey( correlationId ) ) {
      RequestRecord record = requestStatistics.get( correlationId );
      HandlerRecord handlerRecord = new HandlerRecord( handler.getClass( ) );
      record.handlerDownstreamStats.put( handler.getClass( ), handlerRecord );
      return handlerRecord;
    } else {
      return Callables.returning( 0L );
    }
  }
  
}
