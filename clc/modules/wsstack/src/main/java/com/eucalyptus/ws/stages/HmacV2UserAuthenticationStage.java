package com.eucalyptus.ws.stages;

import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.ws.handlers.HmacV2Handler;
import com.eucalyptus.ws.handlers.QueryTimestampHandler;
import com.eucalyptus.ws.handlers.RestfulMarshallingHandler;

public class HmacV2UserAuthenticationStage implements UnrollableStage {

  private boolean doAdmin = false;

  public HmacV2UserAuthenticationStage( ) {
  }

  public HmacV2UserAuthenticationStage( boolean doAdmin ) {
    this.doAdmin = doAdmin;
  }

  @Override
  public String getStageName( ) {
    return "hmac-v2-user-authentication";
  }

  @Override
  public void unrollStage( ChannelPipeline pipeline ) {
    pipeline.addLast( "hmac-v2-verify", new HmacV2Handler( doAdmin ) );
    pipeline.addLast( "timestamp-verify", new QueryTimestampHandler( ) );
  }

  public static class Internal extends HmacV2UserAuthenticationStage {
    public Internal(){
      super(true);
    }
  }
}
