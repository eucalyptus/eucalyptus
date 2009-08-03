package com.eucalyptus.ws.stages;

import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.ws.handlers.HmacV2Handler;
import com.eucalyptus.ws.handlers.QueryTimestampHandler;
import com.eucalyptus.ws.handlers.RestfulMarshallingHandler;
import com.eucalyptus.ws.handlers.WalrusAuthenticationHandler;

public class WalrusOutboundStage implements UnrollableStage {

  @Override
  public String getStageName( ) {
    return "walrus-outbound";
  }

  @Override
  public void unrollStage( ChannelPipeline pipeline ) {
    pipeline.addLast( "walrus-outbound", new WalrusOutboundHandler( ) );
  }

}
