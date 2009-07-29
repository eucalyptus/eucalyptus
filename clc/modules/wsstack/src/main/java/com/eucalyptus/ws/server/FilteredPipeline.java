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

import com.eucalyptus.ws.handlers.ServiceSinkHandler;
import com.eucalyptus.ws.stages.UnrollableStage;

public abstract class FilteredPipeline implements Comparable<FilteredPipeline> {
  private static Logger LOG = Logger.getLogger( FilteredPipeline.class );
  private List<UnrollableStage> stages = new ArrayList<UnrollableStage>( );

  public FilteredPipeline( ) {
    this.addStages( stages );
  }

  public boolean accepts( HttpRequest message ) {
    boolean result = this.checkAccepts( message );
    if( result ) {
      LOG.info("Unrolling pipeline: " + this.getClass( ).getSimpleName( ) );
    }
    return result;
  }
  
  protected abstract boolean checkAccepts( HttpRequest message );

  protected abstract void addStages(List<UnrollableStage> stages);

  public abstract String getPipelineName( );

  public void unroll( ChannelPipeline pipeline ) {
    for( UnrollableStage s : stages ) {
      pipeline.addLast( "pre-" + s.getStageName( ), new StageBottomHandler( s ) );
      s.unrollStage( pipeline );
      pipeline.addLast( "post-" + s.getStageName( ), new StageTopHandler( s ) );
    }
    pipeline.addLast( "service-sink", new ServiceSinkHandler( ) );
    for( Map.Entry<String,ChannelHandler> e : pipeline.toMap( ).entrySet( ) ) {
      LOG.info( " - handler: key=" + e.getKey( ) + " class=" + e.getValue( ).getClass( ).getSimpleName( ) );
    }
  }
  
  @Override
  public int compareTo( final FilteredPipeline o ) {
    return this.getClass( ).getCanonicalName( ).compareTo( o.getClass( ).getCanonicalName( ) );
  }

  public List<UnrollableStage> getStages( ) {
    return this.stages;
  }

  @ChannelPipelineCoverage("one")
  static class StageBottomHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {
    private static Logger LOG = Logger.getLogger( FilteredPipeline.StageBottomHandler.class );
    private UnrollableStage parent;
    
    public StageBottomHandler( UnrollableStage parent ) {
      this.parent = parent;
    }

    @Override
    public void handleDownstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      LOG.info( "END OUTBOUND STAGE: " + parent.getStageName( ) );      
      ctx.sendDownstream( e );
    }

    @Override
    public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      LOG.info( "START INBOUND STAGE: " + parent.getStageName( ) );      
      ctx.sendUpstream( e );
    }
    
  }

  @ChannelPipelineCoverage("one")
  static class StageTopHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {
    private static Logger LOG = Logger.getLogger( FilteredPipeline.StageTopHandler.class );
    private UnrollableStage parent;
    
    public StageTopHandler( UnrollableStage parent ) {
      this.parent = parent;
    }

    @Override
    public void handleDownstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      LOG.info( "START OUTBOUND STAGE: " + parent.getStageName( ) );    
      ctx.sendDownstream( e );
    }

    @Override
    public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      LOG.info( "END INBOUND STAGE: " + parent.getStageName( ) );      
      ctx.sendUpstream( e );
    }
    
  }


}
