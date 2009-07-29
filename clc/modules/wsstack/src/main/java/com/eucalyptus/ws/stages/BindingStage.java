package com.eucalyptus.ws.stages;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.ws.binding.Binding;
import com.eucalyptus.ws.binding.BindingManager;
import com.eucalyptus.ws.handlers.BindingHandler;

public class BindingStage implements UnrollableStage {
  
  @Override
  public void unrollStage( ChannelPipeline pipeline ) {
    pipeline.addLast( "binding", new BindingHandler( ) );
  }

  @Override
  public String getStageName( ) {
    return "message-binding";
  }

}
