package com.eucalyptus.ws.server;

import java.util.List;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.ws.handlers.HeartbeatHandler;
import com.eucalyptus.ws.stages.UnrollableStage;

public class HeartbeatPipeline extends FilteredPipeline {

  @Override
  protected void addStages( List<UnrollableStage> stages ) {
    stages.add( new HeartbeatHandler( ) );
  }

  @Override
  protected boolean checkAccepts( HttpRequest message ) {
    return message.getUri( ).endsWith( "/services/Heartbeat" );
  }

  @Override
  public String getPipelineName( ) {
    return "heartbeat";
  }  

}
