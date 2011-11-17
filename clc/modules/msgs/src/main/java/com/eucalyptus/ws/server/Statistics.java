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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.ws.server;

import java.util.Collection;
import java.util.Map;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

public class Statistics {
  private static Logger                            LOG               = Logger.getLogger( Statistics.class );
  private static final Map<Integer, RequestRecord> requestStatistics = Maps.newConcurrentMap( );
  
  private static class HandlerRecord {
    private final String handlerClassName;
    private final Long   startTime = System.currentTimeMillis( );
    private Long         endTime;
    
    private HandlerRecord( Class<? extends ChannelHandler> class1 ) {
      this.handlerClassName = class1.toString( );
    }
    
    @Override
    public String toString( ) {
      StringBuilder builder = new StringBuilder( );
      builder.append( " " );
      if ( this.handlerClassName != null && this.startTime != null && this.endTime != null ) {
        builder.append( this.handlerClassName ).append( ": " ).append( this.endTime - this.startTime ).append( " msec" );
      }
      return builder.toString( );
    }
    
  }
  
  private static class RequestRecord {
    private final Map<Class, HandlerRecord> handlerUpstreamStats   = Maps.newHashMap( );
    private final Map<Class, HandlerRecord> handlerDownstreamStats = Maps.newHashMap( );
    String                                  type;
    private final long                      creationTime           = System.currentTimeMillis( );
    private volatile HandlerRecord          last;
    
    public Collection<HandlerRecord> values( ) {
      return this.handlerUpstreamStats.values( );
    }
    
    @Override
    public String toString( ) {
      StringBuilder builder = new StringBuilder( );
      if ( this.type != null ) {
        builder.append( this.type ).append( " " ).append( Joiner.on( "\n" + this.type ).join( this.handlerUpstreamStats.values( ) ) );
        builder.append( this.type ).append( " " ).append( Joiner.on( "\n" + this.type ).join( this.handlerDownstreamStats.values( ) ) );
      }
      builder.append( this.type ).append( " " ).append( System.currentTimeMillis( ) - this.creationTime ).append( " msec" );
      return builder.toString( );
    }
    
  }
  
  public static final void startRequest( final Channel channel ) {
    if ( !StackConfiguration.STATISTICS ) {
      return;
    } else {
      final RequestRecord record = new RequestRecord( );
      requestStatistics.put( channel.getId( ), record );
      channel.getCloseFuture( ).addListener( new ChannelFutureListener( ) {
        
        @Override
        public void operationComplete( ChannelFuture future ) throws Exception {
          requestStatistics.remove( channel.getId( ) );
          LOG.debug( record );
        }
      } );
    }
  }
  
  public static final <T extends ChannelHandler> void startUpstream( Integer correlationId, T handler ) {
    if ( !StackConfiguration.STATISTICS ) {
      return;
    } else if ( requestStatistics.containsKey( correlationId ) ) {
      RequestRecord record = requestStatistics.get( correlationId );
      if ( record.last != null ) {
        record.last.endTime = System.currentTimeMillis( );
        record.last = null;
      }
      record.handlerUpstreamStats.put( handler.getClass( ), record.last = new HandlerRecord( handler.getClass( ) ) );
    }
  }
  
  public static final <T extends ChannelHandler> void startDownstream( Integer correlationId, T handler ) {
    if ( !StackConfiguration.STATISTICS ) {
      return;
    } else if ( requestStatistics.containsKey( correlationId ) ) {
      RequestRecord record = requestStatistics.get( correlationId );
      if ( record.last != null ) {
        record.last.endTime = System.currentTimeMillis( );
        record.last = null;
      }
      record.handlerDownstreamStats.put( handler.getClass( ), record.last = new HandlerRecord( handler.getClass( ) ) );
    }
  }
  
}
