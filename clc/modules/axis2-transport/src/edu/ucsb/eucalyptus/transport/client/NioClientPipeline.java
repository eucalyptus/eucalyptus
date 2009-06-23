package edu.ucsb.eucalyptus.transport.client;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import edu.ucsb.eucalyptus.transport.binding.BindingManager;

public class NioClientPipeline implements ChannelPipelineFactory {
  private NioResponseHandler handler;

  public NioClientPipeline( final NioResponseHandler handler ) {
    this.handler = handler;
  }

  public ChannelPipeline getPipeline() throws Exception {
    ChannelPipeline pipeline = Channels.pipeline();

    pipeline.addLast( "decoder", new HttpResponseDecoder() );
    pipeline.addLast( "aggregator", new HttpChunkAggregator( 1048576 ) );
    pipeline.addLast( "encoder", new HttpRequestEncoder() );
    pipeline.addLast( "serializer", new SerializingHandler() );
    pipeline.addLast( "wssec", new WsSecHandler() );
    pipeline.addLast( "soap", new SoapHandler() );
    pipeline.addLast( "binding", new BindingHandler( BindingManager.getBinding( "ec2_amazonaws_com_doc_2008_12_01" ) ) );
    pipeline.addLast( "handler", handler );
    return pipeline;
  }
}
