package com.eucalyptus.ws.stages;

import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.ws.handlers.EucalyptusQueryBinding;
import com.eucalyptus.ws.handlers.RestfulMarshallingHandler;
import com.eucalyptus.ws.handlers.WalrusRESTBinding;

public class WalrusRESTBindingStage implements UnrollableStage {

  @Override
  public String getStageName( ) {
    return "walrus-rest-binding";
  }

  @Override
  public void unrollStage( ChannelPipeline pipeline ) {
    pipeline.addLast( "walrus-rest-binding", new WalrusRESTBinding( ) );
  }

}
