/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
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
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.ws.client.NioMessageReceiver;
import com.eucalyptus.ws.handlers.ServiceSinkHandler;
import com.eucalyptus.ws.stages.UnrollableStage;

public abstract class FilteredPipeline implements Comparable<FilteredPipeline> {
  private static Logger         LOG    = Logger.getLogger( FilteredPipeline.class );
  private List<UnrollableStage> stages = new ArrayList<UnrollableStage>( );
  private NioMessageReceiver msgReceiver;

  public FilteredPipeline( ) {
    this.addStages( stages );
  }

  public FilteredPipeline( NioMessageReceiver msgReceiver ) {
    this.addStages( stages );
    this.msgReceiver = msgReceiver;
  }


  public boolean accepts( HttpRequest message ) {
    boolean result = this.checkAccepts( message );
    if ( result ) {
      LOG.info( "Unrolling pipeline: " + this.getClass( ).getSimpleName( ) );
    }
    return result;
  }

  protected abstract boolean checkAccepts( HttpRequest message );

  protected abstract void addStages( List<UnrollableStage> stages );

  public abstract String getPipelineName( );

  public void unroll( ChannelPipeline pipeline ) {
    try {
      for ( UnrollableStage s : stages ) {
        pipeline.addLast( "pre-" + s.getStageName( ), new StageBottomHandler( s ) );
        s.unrollStage( pipeline );
        pipeline.addLast( "post-" + s.getStageName( ), new StageTopHandler( s ) );
      }
      if( this.msgReceiver != null ){
        pipeline.addLast( "service-sink", new ServiceSinkHandler( this.msgReceiver ) );      
      } else {
        pipeline.addLast( "service-sink", new ServiceSinkHandler( ) );
      }
      for ( Map.Entry<String, ChannelHandler> e : pipeline.toMap( ).entrySet( ) ) {
        LOG.debug( " - handler: key=" + e.getKey( ) + " class=" + e.getValue( ).getClass( ).getSimpleName( ) );
      }
    } catch ( Exception e ) {
      LOG.error("Error unrolling pipeline: " + this.getPipelineName( ) );
      LOG.error( e,e );
      pipeline.getChannel( ).close( );
    }
  }

  @Override
  public int compareTo( final FilteredPipeline o ) {
    return (this.getClass( ).getCanonicalName( ) + this.getPipelineName( ) ).compareTo( (o.getClass( ).getCanonicalName( ) + o.getPipelineName( ) ) );
  }

  public List<UnrollableStage> getStages( ) {
    return this.stages;
  }

  @ChannelPipelineCoverage( "one" )
  static class StageBottomHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {
    private static Logger   LOG = Logger.getLogger( FilteredPipeline.StageBottomHandler.class );
    private UnrollableStage parent;

    public StageBottomHandler( UnrollableStage parent ) {
      this.parent = parent;
    }

    @Override
    public void handleDownstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      if ( e instanceof MessageEvent ) {
        LOG.debug( "END OUTBOUND STAGE: " + parent.getStageName( ) );
      } else {
        LOG.debug( "END OUTBOUND STAGE: " + parent.getStageName( ) + " -- " + e.getClass( ).getSimpleName( ) );
      }
      ctx.sendDownstream( e );
    }

    @Override
    public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      if ( e instanceof MessageEvent ) {
        LOG.debug( "START INBOUND STAGE: " + parent.getStageName( ) );
      } else {
        LOG.debug( "START INBOUND STAGE: " + parent.getStageName( ) + " -- " + e.getClass( ).getSimpleName( ) );
      }
      ctx.sendUpstream( e );
    }
  }

  @ChannelPipelineCoverage( "one" )
  static class StageTopHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {
    private static Logger   LOG = Logger.getLogger( FilteredPipeline.StageTopHandler.class );
    private UnrollableStage parent;

    public StageTopHandler( UnrollableStage parent ) {
      this.parent = parent;
    }

    @Override
    public void handleDownstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      if ( e instanceof MessageEvent ) {
        LOG.debug( "START OUTBOUND STAGE: " + parent.getStageName( ) );
      } else {
        LOG.debug( "START OUTBOUND STAGE: " + parent.getStageName( ) + " -- " + e.getClass( ).getSimpleName( ) );
      }
      ctx.sendDownstream( e );
    }

    @Override
    public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      if ( e instanceof MessageEvent ) {
        LOG.debug( "END INBOUND STAGE: " + parent.getStageName( ) );
      } else {
        LOG.debug( "END INBOUND STAGE: " + parent.getStageName( ) + " -- " + e.getClass( ).getSimpleName( ) );
      }
      ctx.sendUpstream( e );
    }

  }

}
