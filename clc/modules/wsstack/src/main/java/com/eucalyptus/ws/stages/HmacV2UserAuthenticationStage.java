package com.eucalyptus.ws.stages;

import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.ws.handlers.HmacV2Handler;
import com.eucalyptus.ws.handlers.QueryTimestampHandler;
import com.eucalyptus.ws.handlers.RestfulMarshallingHandler;

public class HmacV2UserAuthenticationStage implements UnrollableStage {

  @Override
  public String getStageName( ) {
    return "hmac-v2-user-authentication";
  }

  @Override
  public void unrollStage( ChannelPipeline pipeline ) {
    pipeline.addLast( "hmac-v2-verify", new HmacV2Handler( ) );
    pipeline.addLast( "timestamp-verify", new QueryTimestampHandler( ) );
  }

}
