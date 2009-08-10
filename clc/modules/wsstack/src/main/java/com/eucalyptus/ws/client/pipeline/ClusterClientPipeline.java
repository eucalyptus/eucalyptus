package com.eucalyptus.ws.client.pipeline;

import java.security.GeneralSecurityException;

import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.handlers.soap.AddressingHandler;
import com.eucalyptus.ws.handlers.wssecurity.ClusterWsSecHandler;

public class ClusterClientPipeline extends NioClientPipeline {
  public ClusterClientPipeline( final NioResponseHandler handler ) throws GeneralSecurityException {
    super( handler, "eucalyptus_ucsb_edu", new ClusterWsSecHandler( ) );
  }

  @Override
  public ChannelPipeline getPipeline( ) throws Exception {
    ChannelPipeline pipeline = super.getPipeline( );
    pipeline.addBefore( "soap", "addressing", new AddressingHandler( ) );
    return pipeline;
  }
  
  
}
