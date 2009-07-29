package com.eucalyptus.ws.util;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.ws.server.DuplicatePipelineException;
import com.eucalyptus.ws.server.FilteredPipeline;
import com.eucalyptus.ws.server.NoAcceptingPipelineException;

public class PipelineRegistry {
  private static PipelineRegistry registry;

  public static PipelineRegistry getInstance( ) {
    synchronized ( PipelineRegistry.class ) {
      if ( PipelineRegistry.registry == null ) {
        PipelineRegistry.registry = new PipelineRegistry( );
      }
    }
    return PipelineRegistry.registry;
  }

  private final NavigableSet<FilteredPipeline> pipelines = new ConcurrentSkipListSet<FilteredPipeline>( );

  public void register( final FilteredPipeline pipeline ) {
    this.pipelines.add( pipeline );
  }

  public FilteredPipeline find( final HttpRequest request ) throws DuplicatePipelineException, NoAcceptingPipelineException {
    FilteredPipeline candidate = null;
    for ( final FilteredPipeline f : this.pipelines ) {
      if ( f.accepts( request ) ) {
        if ( candidate != null ) { throw new DuplicatePipelineException( ); }
        candidate = f;
      }
    }
    if( candidate == null ) {
      throw new NoAcceptingPipelineException();
    }
    return candidate;
  }

}
