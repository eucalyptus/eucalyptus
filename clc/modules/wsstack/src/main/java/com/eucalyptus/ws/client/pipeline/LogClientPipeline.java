package com.eucalyptus.ws.client.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;

import com.eucalyptus.ws.handlers.NioHttpResponseDecoder;
import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.eucalyptus.ws.handlers.http.NioHttpRequestEncoder;
import com.eucalyptus.ws.handlers.soap.AddressingHandler;
import com.eucalyptus.ws.handlers.soap.SoapHandler;

public class LogClientPipeline extends NioClientPipeline {
  public LogClientPipeline( final NioResponseHandler handler ) {
    super( handler, "eucalyptus_ucsb_edu" );
  }

  @Override
  public ChannelPipeline getPipeline( ) throws Exception {
    ChannelPipeline pipeline = Channels.pipeline( );
    pipeline.addLast( "decoder", new NioHttpResponseDecoder( ) );
    pipeline.addLast( "aggregator", new HttpChunkAggregator( 1048576 ) );
    pipeline.addLast( "encoder", new NioHttpRequestEncoder( ) );
    pipeline.addLast( "serializer", new SoapMarshallingHandler( ) );
    pipeline.addLast( "addressing", new AddressingHandler( "EucalyptusGL#" ) );
    pipeline.addLast( "soap", new SoapHandler( ) );
    pipeline.addLast( "binding", this.getBindingHandler( ) );
    pipeline.addLast( "handler", this.getHandler( ) );
    return pipeline;
  }

}
