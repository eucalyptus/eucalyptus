package com.eucalyptus.ws.server;

import java.util.List;
import java.util.Set;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.ws.stages.SoapUserAuthenticationStage;
import com.eucalyptus.ws.stages.BindingStage;
import com.eucalyptus.ws.stages.UnrollableStage;
import com.eucalyptus.ws.stages.WalrusSoapUserAuthenticationStage;

public class WalrusSoapPipeline extends FilteredPipeline {

  @Override
  public boolean checkAccepts( final HttpRequest message ) {
    return message.getUri( ).endsWith( "/services/Walrus" ) && message.getHeaderNames().contains( "SOAPAction" );
  }

  @Override
  public String getPipelineName( ) {
    return "walrus-soap";
  }

  @Override
  protected void addStages( List<UnrollableStage> stages ) {
    stages.add( new WalrusSoapUserAuthenticationStage( ) );
    stages.add( new BindingStage( ) );
  }

}
