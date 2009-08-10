package com.eucalyptus.ws.stages;

import java.security.GeneralSecurityException;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;

import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.eucalyptus.ws.handlers.soap.AddressingHandler;
import com.eucalyptus.ws.handlers.soap.SoapHandler;
import com.eucalyptus.ws.handlers.wssecurity.InternalWsSecHandler;
import com.eucalyptus.ws.handlers.wssecurity.UserWsSecHandler;

public class SoapInternalAuthenticationStage implements UnrollableStage {
  private static Logger LOG = Logger.getLogger( SoapInternalAuthenticationStage.class );

  @Override
  public String getStageName( ) {
    return "soap-internal-authentication";
  }

  @Override
  public void unrollStage( ChannelPipeline pipeline ) {
    pipeline.addLast( "deserialize", new SoapMarshallingHandler( ) );
    try {
      pipeline.addLast( "ws-security", new InternalWsSecHandler( ) );
    } catch ( GeneralSecurityException e ) {
      LOG.error(e,e);
    }
    pipeline.addLast( "ws-addressing", new AddressingHandler( ) );
    pipeline.addLast( "build-soap-envelope", new SoapHandler( ) );
  }

}
