package com.eucalyptus.ws.stages;

import org.jboss.netty.channel.ChannelPipeline;

public interface UnrollableStage {
    
  public void unrollStage( ChannelPipeline pipeline );
  public String getStageName();
  
  

}
