package com.eucalyptus.ws.stages;

import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.eucalyptus.ws.handlers.soap.SoapHandler;
import com.eucalyptus.ws.handlers.wssecurity.InternalWsSecHandler;
import com.eucalyptus.ws.handlers.wssecurity.UserWsSecHandler;

public class SoapUserAuthenticationStage implements UnrollableStage {

  @Override
  public void unrollStage( final ChannelPipeline pipeline ) {
    pipeline.addLast( "deserialize", new SoapMarshallingHandler( ) );
    pipeline.addLast( "build-soap-envelope", new SoapHandler( ) );
    pipeline.addLast( "ws-security", new UserWsSecHandler( ) );
  }

  @Override
  public String getStageName( ) {
    return "soap-user-authentication";
  }
  
}
