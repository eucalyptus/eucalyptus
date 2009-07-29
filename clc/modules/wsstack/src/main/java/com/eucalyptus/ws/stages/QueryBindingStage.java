package com.eucalyptus.ws.stages;

import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.ws.handlers.EucalyptusQueryBinding;
import com.eucalyptus.ws.handlers.RestfulMarshallingHandler;

public class QueryBindingStage implements UnrollableStage {

  @Override
  public String getStageName( ) {
    return "query-binding";
  }

  @Override
  public void unrollStage( ChannelPipeline pipeline ) {
    pipeline.addLast( "restful-binding", new EucalyptusQueryBinding( ) );
  }

}
