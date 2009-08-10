package com.eucalyptus.ws.client.pipeline;

import java.security.GeneralSecurityException;

import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.handlers.soap.AddressingHandler;
import com.eucalyptus.ws.handlers.wssecurity.InternalWsSecHandler;

public class InternalClientPipeline extends NioClientPipeline {
  public InternalClientPipeline( final NioResponseHandler handler ) throws GeneralSecurityException {
    super( handler, "msgs_eucalyptus_ucsb_edu", new InternalWsSecHandler( ) );
  }
  
  @Override
  public ChannelPipeline getPipeline( ) throws Exception {
    ChannelPipeline pipeline = super.getPipeline( );
    pipeline.addBefore( "soap", "addressing", new AddressingHandler( ) );
    return pipeline;
  }

}
