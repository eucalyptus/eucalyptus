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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
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
      LOG.info( " - handler: key=" + e.getKey( ) + " class=" + e.getValue( ).getClass( ).getSimpleName( ) );
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
        LOG.info( "END OUTBOUND STAGE: " + parent.getStageName( ) );
      } else {
        LOG.info( "END OUTBOUND STAGE: " + parent.getStageName( ) + " -- " + e.getClass( ).getSimpleName( ) );
      }
      ctx.sendDownstream( e );
    }

    @Override
    public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      if ( e instanceof MessageEvent ) {
        LOG.info( "START INBOUND STAGE: " + parent.getStageName( ) );
      } else {
        LOG.info( "START INBOUND STAGE: " + parent.getStageName( ) + " -- " + e.getClass( ).getSimpleName( ) );
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
        LOG.info( "START OUTBOUND STAGE: " + parent.getStageName( ) );
      } else {
        LOG.info( "START OUTBOUND STAGE: " + parent.getStageName( ) + " -- " + e.getClass( ).getSimpleName( ) );
      }
      ctx.sendDownstream( e );
    }

    @Override
    public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      if ( e instanceof MessageEvent ) {
        LOG.info( "END INBOUND STAGE: " + parent.getStageName( ) );
      } else {
        LOG.info( "END INBOUND STAGE: " + parent.getStageName( ) + " -- " + e.getClass( ).getSimpleName( ) );
      }
      ctx.sendUpstream( e );
    }

  }

}
