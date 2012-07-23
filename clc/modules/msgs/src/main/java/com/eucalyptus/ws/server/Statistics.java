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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
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

public class Statistics {
  private static Logger                            LOG               = Logger.getLogger( Statistics.class );
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
