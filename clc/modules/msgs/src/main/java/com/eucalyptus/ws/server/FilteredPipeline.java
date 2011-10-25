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
 * THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.server;

import java.util.Map;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpRequest;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Filterable;
import com.eucalyptus.util.HasName;
import com.eucalyptus.ws.Handlers;

public abstract class FilteredPipeline implements HasName<FilteredPipeline>, Filterable<HttpRequest> {
  private static Logger LOG = Logger.getLogger( FilteredPipeline.class );
  
  protected abstract static class InternalPipeline extends FilteredPipeline {
    private final ComponentId componentId;
    
    InternalPipeline( ComponentId componentId ) {
      this.componentId = componentId;
    }
    
    @Override
    protected void addSystemHandlers( ChannelPipeline pipeline ) {
      Handlers.addInternalSystemHandlers( pipeline );
      Handlers.addSystemHandlers( pipeline );
    }
    
    private ComponentId getComponentId( ) {
      return this.componentId;
    }
    
  }
  
  public FilteredPipeline( ) {}
  
  protected void addSystemHandlers( ChannelPipeline pipeline ) {
    Handlers.addSystemHandlers( pipeline );
  }
  
  public abstract String getName( );
  
  public void unroll( final ChannelPipeline pipeline ) {
    try {
      this.addHandlers( pipeline );
      if ( Logs.isExtrrreeeme( ) ) {
        for ( final Map.Entry<String, ChannelHandler> e : pipeline.toMap( ).entrySet( ) ) {
          EventRecord.here( this.getClass( ), EventType.PIPELINE_HANDLER, e.getKey( ), e.getValue( ).getClass( ).getSimpleName( ) ).trace( );
        }
      }
    } catch ( final Exception e ) {
      LOG.error( "Error unrolling pipeline: " + this.getName( ) );
      final ChannelFuture close = pipeline.getChannel( ).close( );
      close.awaitUninterruptibly( );
      LOG.error( "Forced pipeline to close due to exception: ", e );
    }
  }
  
  public abstract ChannelPipeline addHandlers( ChannelPipeline pipeline );
  
  @Override
  public abstract boolean checkAccepts( HttpRequest message );
  
  @Override
  public final int compareTo( final FilteredPipeline o ) {
    return this.getName( ).compareTo( this.getName( ) );
  }
  
  @Override
  public final int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.getName( ) == null )
      ? 0
      : this.getName( ).hashCode( ) );
    return result;
  }
  
  @Override
  public final boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    FilteredPipeline other = ( FilteredPipeline ) obj;
    return this.compareTo( other ) == 0;
  }
  
  @Override
  public String toString( ) {
    return String.format( "FilteredPipeline:name=%s:hashCode=%s:class=%s", this.getName( ), this.hashCode( ), this.getClass( ).getSimpleName( ) );
  }
  
}
