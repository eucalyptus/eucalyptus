package com.eucalyptus.ws.stages;
import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.ws.handlers.MessageStackHandler;



public class ElasticFoxMangleStage implements UnrollableStage {

  @Override
  public String getStageName( ) {
    return "elasticfox-mangler";
  }

  @Override
  public void unrollStage( ChannelPipeline pipeline ) {
    
  }
  
}
