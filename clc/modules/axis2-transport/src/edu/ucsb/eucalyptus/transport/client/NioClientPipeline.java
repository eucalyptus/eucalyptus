package edu.ucsb.eucalyptus.transport.client;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;

public class NioClientPipeline implements ChannelPipelineFactory {
  private NioResponseHandler handler;

  public NioClientPipeline( final NioResponseHandler handler ) {
    this.handler = handler;
  }

  public ChannelPipeline getPipeline() throws Exception {
    ChannelPipeline pipeline = Channels.pipeline();
    pipeline.addLast("decoder", new HttpResponseDecoder());
    //Uncomment the following line if you don't want to handle HttpChunks.
    pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
    pipeline.addLast("encoder", new HttpRequestEncoder());
    pipeline.addLast("handler", handler);
    return pipeline;

  }
}
