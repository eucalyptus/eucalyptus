package com.eucalyptus.ws.stages;

import org.jboss.netty.channel.ChannelPipeline;

public class SoapInternalAuthenticationStage implements UnrollableStage {

  @Override
  public String getStageName( ) {
    return "soap-internal-authentication";
  }

  @Override
  public void unrollStage( ChannelPipeline pipeline ) {
    // TODO Auto-generated method stub

  }

}
