package com.eucalyptus.ws.client.pipeline;

import java.security.GeneralSecurityException;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;

import com.eucalyptus.ws.handlers.NioHttpResponseDecoder;
import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.eucalyptus.ws.handlers.http.NioHttpRequestEncoder;
import com.eucalyptus.ws.handlers.soap.AddressingHandler;
import com.eucalyptus.ws.handlers.soap.SoapHandler;
import com.eucalyptus.ws.handlers.wssecurity.ClusterWsSecHandler;

public class ClusterClientPipeline extends NioClientPipeline {
  public ClusterClientPipeline( final NioResponseHandler handler ) throws GeneralSecurityException {
    super( handler, "eucalyptus_ucsb_edu", new ClusterWsSecHandler( ) );
  }

  @Override
  public ChannelPipeline getPipeline( ) throws Exception {
    ChannelPipeline pipeline = Channels.pipeline( );
    pipeline.addLast( "decoder", new NioHttpResponseDecoder( ) );
    pipeline.addLast( "aggregator", new HttpChunkAggregator( 1048576 ) );
    pipeline.addLast( "encoder", new NioHttpRequestEncoder( ) );
    pipeline.addLast( "serializer", new SoapMarshallingHandler( ) );
    pipeline.addLast( "wssec", this.getWssecHandler( ) );
    pipeline.addLast( "addressing", new AddressingHandler( "EucalyptusCC#" ) );
    pipeline.addLast( "soap", new SoapHandler( ) );
    pipeline.addLast( "binding", this.getBindingHandler( ) );
    pipeline.addLast( "handler", this.getHandler( ) );
    return pipeline;
  }

  
}
